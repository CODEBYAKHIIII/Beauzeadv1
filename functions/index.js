const crypto = require("node:crypto");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const { defineSecret } = require("firebase-functions/params");
const { HttpsError, onCall } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");

initializeApp();

const resendApiKey = defineSecret("RESEND_API_KEY");
const resendFromEmail = defineSecret("RESEND_FROM_EMAIL");
const firestore = getFirestore();
const OTP_LIFETIME_MS = 10 * 60 * 1000;
const RESEND_INTERVAL_MS = 60 * 1000;
const MAX_OTP_ATTEMPTS = 5;

function requireVerifiedUser(request) {
  if (!request.auth || request.auth.token.email_verified !== true) {
    throw new HttpsError("unauthenticated", "Sign in with a verified email account.");
  }
  return request.auth;
}

function challengeDocument(uid) {
  return firestore.collection("_accountDeletionChallenges").doc(uid);
}

function otpDigest(uid, otp) {
  return crypto
    .createHmac("sha256", resendApiKey.value())
    .update(`${uid}:${otp}`)
    .digest("hex");
}

function digestMatches(uid, otp, expectedDigest) {
  const actual = Buffer.from(otpDigest(uid, otp), "hex");
  const expected = Buffer.from(expectedDigest, "hex");
  return actual.length === expected.length && crypto.timingSafeEqual(actual, expected);
}

exports.sendAccountDeletionOtp = onCall(
  { region: "us-central1", secrets: [resendApiKey, resendFromEmail] },
  async (request) => {
    const auth = requireVerifiedUser(request);
    const email = auth.token.email;
    const reason = request.data?.reason;
    if (typeof email !== "string" || !email || typeof reason !== "string" || reason.length > 350) {
      throw new HttpsError("invalid-argument", "A valid account and reason are required.");
    }

    const otp = crypto.randomInt(0, 1_000_000).toString().padStart(6, "0");
    const now = Date.now();
    const challengeRef = challengeDocument(auth.uid);
    await firestore.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(challengeRef);
      const sentAt = snapshot.exists ? snapshot.get("sentAt")?.toMillis() : undefined;
      if (sentAt && now - sentAt < RESEND_INTERVAL_MS) {
        throw new HttpsError("resource-exhausted", "Wait 60 seconds before requesting another code.");
      }

      transaction.set(challengeRef, {
        otpDigest: otpDigest(auth.uid, otp),
        reason,
        sentAt: Timestamp.fromMillis(now),
        expiresAt: Timestamp.fromMillis(now + OTP_LIFETIME_MS),
        attempts: 0,
        verified: false
      });
    });

    let response;
    try {
      response = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${resendApiKey.value()}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          from: resendFromEmail.value(),
          to: [email],
          subject: "Your Waves account deletion code",
          text: `Your Waves account deletion verification code is ${otp}. It expires in 10 minutes. If you did not request account deletion, you can ignore this email.`
        })
      });
    } catch (error) {
      await challengeRef.delete();
      logger.error("Unable to contact the email delivery service.", { uid: auth.uid });
      throw new HttpsError("unavailable", "The verification email could not be sent. Please try again.");
    }

    if (!response.ok) {
      await challengeRef.delete();
      logger.error("Email delivery service rejected a deletion-code email.", {
        uid: auth.uid,
        status: response.status
      });
      throw new HttpsError("unavailable", "The verification email could not be sent. Please try again.");
    }

    return { sent: true };
  }
);

exports.completeAccountDeletion = onCall(
  { region: "us-central1", secrets: [resendApiKey] },
  async (request) => {
    const auth = requireVerifiedUser(request);
    const otp = request.data?.otp;
    if (typeof otp !== "string" || !/^\d{6}$/.test(otp)) {
      throw new HttpsError("invalid-argument", "Enter the six-digit verification code.");
    }

    const challengeRef = challengeDocument(auth.uid);
    const verificationResult = await firestore.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(challengeRef);
      if (!snapshot.exists) {
        return "missing";
      }

      const challenge = snapshot.data();
      if (challenge.expiresAt.toMillis() <= Date.now()) {
        transaction.delete(challengeRef);
        return "expired";
      }
      if (challenge.attempts >= MAX_OTP_ATTEMPTS) {
        transaction.delete(challengeRef);
        return "locked";
      }
      if (!digestMatches(auth.uid, otp, challenge.otpDigest)) {
        const attempts = challenge.attempts + 1;
        if (attempts >= MAX_OTP_ATTEMPTS) {
          transaction.delete(challengeRef);
          return "locked";
        }
        transaction.update(challengeRef, { attempts });
        return "incorrect";
      }

      transaction.update(challengeRef, { verified: true });
      return "valid";
    });

    if (verificationResult === "missing" || verificationResult === "expired") {
      throw new HttpsError("failed-precondition", "The verification code is missing or expired.");
    }
    if (verificationResult === "locked") {
      throw new HttpsError("resource-exhausted", "Too many incorrect codes. Request a new code.");
    }
    if (verificationResult === "incorrect") {
      throw new HttpsError("permission-denied", "The verification code is incorrect.");
    }

    const userRef = firestore.collection("users").doc(auth.uid);
    await getStorage().bucket().deleteFiles({ prefix: `users/${auth.uid}/` });
    await firestore.recursiveDelete(userRef);
    await getAuth().deleteUser(auth.uid);
    await challengeRef.delete();
    return { deleted: true };
  }
);
