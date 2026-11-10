// Auto Contact Saver - Unified VPS Production Server
// Combines Razorpay Order Creation, Signature Verification, Admin Mission Control, and Firestore Sync
// Deploy: Run 'node index.js' or 'pm2 start index.js --name autocontact-server'

const http = require('http');
const https = require('https');
const crypto = require('crypto');
const url = require('url');

const PORT = process.env.PORT || 3000;

// Razorpay Credentials
const RAZORPAY_KEY_ID = "rzp_live_TI2wdFKYDJdAxY";
const RAZORPAY_KEY_SECRET = "0pNQOQBRWxmtdE8mPVLlvYfi";

// Firebase Service Account Credentials
const FIREBASE_SERVICE_ACCOUNT = {
  type: "service_account",
  project_id: "auto-contact-saver-b8a12",
  private_key_id: "4118274f7c7ecca19eb86d5430599592024b20f1",
  private_key: "-----BEGIN PRIVATE KEY-----\nMIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDcZfGbS92YFvne\nQnE8G5dlBs00it0EjqVn5XVvKIxynn2mBJKB5b5QzxNA3rlYsjytOqaL8tlmMHRz\nhUykA4FGt6HODF+Dt3u06vsQIOj9uDoAUZzJmlBvUJqRAaZ7GjjWULxhVR51DBYA\nsclKIu8VhajrrLrvgXBIgpFLVEgtTh7RAjHpZCoEvAeWDDctmp+GaQ4ib7ZVd09J\nhhC5uPx3CSfeh6WPJ1IbySbCstj8O714DnHv1JixaF0tMHTInGbJnZ8bVPXuRKX1\nvlVG5nyKuIHB2yIWc9uY2ls8EVis/NYCjGZN9ZUbmo1B0oojQRBty+oV0vOrRNgJ\nnthhjCmdAgMBAAECggEAEz4rXB3BmTfYVRmD/XWDQGTZoc7GAVHbCzkziaMGsDM6\nUTZw/Dps5W1PBwjrMd1OAunuBKYNaSit4jAvQtqzfNMno2/dHwj0GhWyGfRlo1Fk\nbwaxI8D/aNimxiZYvMLLV5rw96aQ5DNL8H+VTY3DRiPHXdxU7fhlL0+HFAalzp6r\n/y6kmzn4xB4aCDyc7c3geL5j57dFsEoYDFbhJnYHo2Q+h7dy6U2yMnsdwMC3nHaI\nnBgeK3KK6f7KmyFQqr5LUrSdMJAj7bOe3nZ83NbHNqrHxIDEsvBb1vQb3Vxvllal\n5s5egQ7LxU/AefU35doY0O4TWAnKZRL2dP4g1HdagQKBgQD0hyktLZdJhNl5NMOn\n2t4eOhHa2xZa/4X81CHF02tq51gkJlBXORtJxYLb1ppIMu+cIv7R3CPcAeM6urUY\nxlT3fnh1/BT+wXB6CJh9P1dc/MU06nlCAbW5wEbOPpEMbOWpMXeq/Qr/HCL3qA2f\nLuPV8ZYpL7tEG3/cPMmbhG77sQKBgQDmvPqVKKuuNU9bLsnOHnHHEHDDKBFxMDEx\n9U42y9qVWGYI0lhLtSLfQhU/YqKGtjtZf0M+NMQMYzUc/hKthZdtGe81mLP/gPWc\nfMptIcrknM3g3zuGQcr7fX0k8xuuSPA9qcmZm6mtKxCacg3ZjDXqzYG1h6QmFTYy\nHbIremkDrQKBgQCTW5he12iiL2L2/esijmzofkogP4MbV30hPZ0UoDHNfJjLQCOO\nTKW7MUGIxay38aofx58UTLt3a9PJopZb1bmCZD6nqz/jviLQB7oNUEyPhuw41Uw5\nkNo4lDyXMXErsbw818irzE4CKqyP3kBHO3Sliy7YpyENDB1jVWaRR5+yIQKBgQCh\nlmbGXp0ceyj/6lxZhOiZR+IwSbctVVqeafREW67QN//83ZIIMuog/95xbVn0jVoy\n6vTkc394TWG81BGwg4XwWeEz36TuohlfZeU9WBUG1Ok5OhwtgZXtWYxye+4+t8XG\n6UjN0j2ZkDcocgdQ+ugnb5QO1NcX9HLn1fQYjrlMYQKBgQCoG0JAV6lAy/dVdid8\nmpzP272NXWIFVH4+1PJfr/Wz22QsbiqwH8I7OnLBOoLfLySL368bKhNHfedbzl2W\n3osUzxu7u7BCuETfboYfqL634j9qtY0oK7yeNGA/EEss77/5O0/CXUqtorXBpPR2\n7VvN1qPXb3dLLrdBsySQpVfQZg==\n-----END PRIVATE KEY-----\n",
  client_email: "firebase-adminsdk-fbsvc@auto-contact-saver-b8a12.iam.gserviceaccount.com",
  client_id: "106184732523133790059",
  auth_uri: "https://accounts.google.com/o/oauth2/auth",
  token_uri: "https://oauth2.googleapis.com/token",
};

// Auth Accounts (Owner vs Marketing)
const AUTH_ACCOUNTS = [
  {
    email: "owner@autocontactsaver.com",
    pin: "80978097",
    role: "OWNER",
    name: "Super Admin (Owner)"
  },
  {
    email: "marketing@autocontactsaver.com",
    pin: "654321",
    role: "MARKETING",
    name: "Marketing Team"
  }
];

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, GET, OPTIONS, PATCH, DELETE",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
};

function sendJson(res, data, status = 200) {
  res.writeHead(status, { "Content-Type": "application/json", ...corsHeaders });
  res.end(JSON.stringify(data));
}

function sendHtml(res, html, status = 200) {
  res.writeHead(status, {
    "Content-Type": "text/html; charset=utf-8",
    "Cache-Control": "no-cache, no-store, must-revalidate",
    ...corsHeaders
  });
  res.end(html);
}

function parseJson(req) {
  return new Promise((resolve) => {
    let body = "";
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (e) {
        resolve({});
      }
    });
    req.on('error', () => resolve({}));
  });
}

function normalizePlanType(plan) {
  if (!plan) return "TRIAL";
  const p = String(plan).toUpperCase().trim();
  if (p.includes("LIFE") || p.includes("1999")) return "LIFETIME";
  if (p.includes("YEAR") || p.includes("ANNUAL") || p.includes("365") || p.includes("999")) return "YEARLY";
  if (p.includes("MONTH") || p.includes("30") || p.includes("199")) return "MONTHLY";
  return "TRIAL";
}

function extractYouTubeVideoId(urlOrId) {
  if (!urlOrId) return "";
  const trimmed = String(urlOrId).trim();
  if (/^[a-zA-Z0-9_-]{11}$/.test(trimmed)) {
    return trimmed;
  }
  const match = trimmed.match(/(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|shorts\/|live\/|watch\?.+&v=))([\w-]{11})/);
  return match ? match[1] : trimmed;
}

// ─────────────────────────────────────────────────────────────────
// Token Auth Helpers (Base64 HMAC Simulation for Worker)
// ─────────────────────────────────────────────────────────────────
function generateAuthToken(account) {
  const payload = {
    email: account.email,
    role: account.role,
    name: account.name,
    iat: Date.now(),
    exp: Date.now() + (30 * 24 * 60 * 60 * 1000) // 30 days session
  };
  const str = JSON.stringify(payload);
  return btoa(unescape(encodeURIComponent(str)));
}

function verifyAuthToken(authHeader) {
  if (!authHeader) return null;
  const parts = authHeader.split(" ");
  const token = parts.length === 2 ? parts[1] : parts[0];
  if (!token) return null;

  try {
    const decodedStr = decodeURIComponent(escape(atob(token)));
    const payload = JSON.parse(decodedStr);
    if (payload.exp && Date.now() > payload.exp) return null;
    
    // Check if account still exists
    const acc = AUTH_ACCOUNTS.find(a => a.email.toLowerCase() === payload.email.toLowerCase());
    if (!acc) return null;

    return {
      email: acc.email,
      role: acc.role,
      name: acc.name
    };
  } catch (e) {
    return null;
  }
}

async function updateAndVerifyFirestorePlanWithRetry(userId, planId, paymentId, orderId, email, isAdminManual = false) {
  const maxRetries = 3;
  let lastError = null;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
      const now = Date.now();

      const expiryDate = (planId === "LIFETIME") ? 0 : (now + (planId === "YEARLY" ? 31536000000 : 2592000000));
      const provider = isAdminManual ? "ADMIN_MANUAL" : "RAZORPAY";

      const firestoreUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/users/${userId}?updateMask.fieldPaths=planType&updateMask.fieldPaths=purchaseDate&updateMask.fieldPaths=expiryDate&updateMask.fieldPaths=isActive&updateMask.fieldPaths=paymentId&updateMask.fieldPaths=orderId&updateMask.fieldPaths=paymentProvider&updateMask.fieldPaths=status&updateMask.fieldPaths=lastUpdated`;

      const updatePayload = {
        fields: {
          userId: { stringValue: userId },
          planType: { stringValue: planId },
          status: { stringValue: "ACTIVE" },
          paymentProvider: { stringValue: provider },
          paymentId: { stringValue: paymentId || "" },
          orderId: { stringValue: orderId || "" },
          purchaseDate: { integerValue: now.toString() },
          expiryDate: { integerValue: expiryDate.toString() },
          isActive: { booleanValue: true },
          lastUpdated: { integerValue: now.toString() }
        }
      };

      if (email) {
        updatePayload.fields.email = { stringValue: email };
      }

      const patchResponse = await fetch(firestoreUrl, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${accessToken}`
        },
        body: JSON.stringify(updatePayload)
      });

      if (!patchResponse.ok) {
        const errorText = await patchResponse.text();
        throw new Error(`Firestore PATCH failed: ${errorText}`);
      }

      return {
        success: true,
        attempt: attempt,
        planType: planId,
        isActive: true,
        expiryDate: expiryDate,
        message: `Plan ${planId} activated successfully for user ${userId}`
      };

    } catch (err) {
      lastError = err.message;
      if (attempt < maxRetries) {
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
    }
  }

  return { success: false, error: lastError || "Failed after retries" };
}

async function fetchAllUsersFromFirestore(force = false) {
  const now = Date.now();
  if (!force && memoryCacheUsers && (now - lastCacheTime < CACHE_TTL_MS)) {
    return memoryCacheUsers;
  }

  try {
    const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
    let allDocs = [];
    let pageToken = null;

    do {
      let url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/users?pageSize=300`;
      if (pageToken) {
        url += `&pageToken=${encodeURIComponent(pageToken)}`;
      }

      const resp = await fetch(url, {
        headers: { "Authorization": `Bearer ${accessToken}` }
      });

      if (!resp.ok) {
        const errText = await resp.text();
        if (resp.status === 429 || errText.includes("RESOURCE_EXHAUSTED") || errText.includes("Quota exceeded")) {
          console.warn("Firestore Quota (429 RESOURCE_EXHAUSTED) hit. Serving cached / fallback snapshot.");
          if (memoryCacheUsers && memoryCacheUsers.length > 0) return memoryCacheUsers;
          return FALLBACK_USERS_SNAPSHOT;
        }
        throw new Error(`Firestore list users failed: ${errText}`);
      }

      const data = await resp.json();
      if (data.documents && Array.isArray(data.documents)) {
        allDocs.push(...data.documents);
      }
      pageToken = data.nextPageToken || null;
    } while (pageToken);

    const users = allDocs.map(doc => {
      const fields = doc.fields || {};
      const docPathParts = doc.name.split('/');
      const docId = docPathParts[docPathParts.length - 1];

      const parseValue = (f) => {
        if (!f) return null;
        if (f.stringValue !== undefined) return f.stringValue;
        if (f.booleanValue !== undefined) return f.booleanValue;
        if (f.integerValue !== undefined) return parseInt(f.integerValue, 10);
        if (f.doubleValue !== undefined) return parseFloat(f.doubleValue);
        if (f.timestampValue !== undefined) return new Date(f.timestampValue).getTime();
        return null;
      };

      const rawPlan = parseValue(fields.planType);
      const planType = normalizePlanType(rawPlan);
      const isActive = parseValue(fields.isActive) === true || parseValue(fields.status) === "ACTIVE";
      const expiryDate = parseValue(fields.expiryDate) || 0;
      const curTime = Date.now();
      const isExpired = planType !== "LIFETIME" && expiryDate > 0 && curTime > expiryDate;

      let computedStatus = "TRIAL";
      if (planType !== "TRIAL") {
        if (!isActive) computedStatus = "DEACTIVATED";
        else if (isExpired) computedStatus = "EXPIRED";
        else computedStatus = "ACTIVE";
      }

      return {
        userId: parseValue(fields.userId) || docId,
        name: parseValue(fields.name) || parseValue(fields.displayName) || "App User",
        email: parseValue(fields.email) || "",
        mobileNumber: parseValue(fields.mobileNumber) || "",
        countryCode: parseValue(fields.countryCode) || "+91",
        fullPhone: parseValue(fields.fullPhone) || parseValue(fields.mobileNumber) || "",
        planType: planType,
        isActive: isActive,
        isExpired: isExpired,
        status: computedStatus,
        contactsSaved: parseValue(fields.contactsSaved) || 0,
        trialContactsLimit: parseValue(fields.trialContactsLimit) || 100,
        createdAt: parseValue(fields.createdAt) || parseValue(fields.lastUpdated) || parseValue(fields.purchaseDate) || 0,
        expiryDate: expiryDate,
        purchaseDate: parseValue(fields.purchaseDate) || parseValue(fields.paymentDate) || 0,
        paymentProvider: parseValue(fields.paymentProvider) || "NONE",
        paymentId: parseValue(fields.paymentId) || parseValue(fields.razorpayPaymentId) || "",
        orderId: parseValue(fields.orderId) || parseValue(fields.razorpayOrderId) || "",
        amountPaid: parseValue(fields.amountPaid) || (PLAN_PRICING[planType] || 0)
      };
    });

    memoryCacheUsers = users;
    lastCacheTime = Date.now();
    return users;
  } catch (err) {
    if (err.message && (err.message.includes("Quota exceeded") || err.message.includes("RESOURCE_EXHAUSTED"))) {
      console.warn("Caught Firestore quota error, returning cached / fallback snapshot.");
      if (memoryCacheUsers && memoryCacheUsers.length > 0) return memoryCacheUsers;
      return FALLBACK_USERS_SNAPSHOT;
    }
    throw err;
  }
}

// ─────────────────────────────────────────────────────────────────
// Helper: Compute Advanced Analytics (Role Aware + Month-Wise Sales)
// ─────────────────────────────────────────────────────────────────
function computeAnalytics(users, role = "OWNER") {
  const totalUsers = users.length;
  const now = Date.now();
  const sevenDaysFromNow = now + (7 * 24 * 60 * 60 * 1000);

  let trialCount = 0;
  let monthlyCount = 0;
  let yearlyCount = 0;
  let lifetimeCount = 0;
  let activePaidCount = 0;
  let expiredPaidCount = 0;
  let expiringSoonCount = 0;
  let grossRevenue = 0;
  let mrr = 0;
  let phoneLeadsCount = 0;
  let totalContactsSavedByLeads = 0;

  const planBreakdown = { MONTHLY: 0, YEARLY: 0, LIFETIME: 0, TRIAL: 0 };
  const dailyTrends = {};
  const monthlySalesMap = {};

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];

  users.forEach(u => {
    const plan = normalizePlanType(u.planType);
    planBreakdown[plan] = (planBreakdown[plan] || 0) + 1;

    if (plan === "TRIAL") {
      trialCount++;
      if (u.fullPhone || u.mobileNumber) phoneLeadsCount++;
      totalContactsSavedByLeads += (u.contactsSaved || 0);
    } else {
      const price = u.amountPaid || PLAN_PRICING[plan] || 0;
      grossRevenue += price;

      if (plan === "MONTHLY") {
        monthlyCount++;
        mrr += 199;
      } else if (plan === "YEARLY") {
        yearlyCount++;
        mrr += Math.round(999 / 12);
      } else if (plan === "LIFETIME") {
        lifetimeCount++;
      }

      if (u.status === "ACTIVE") {
        activePaidCount++;
        if (u.expiryDate > now && u.expiryDate <= sevenDaysFromNow) {
          expiringSoonCount++;
        }
      } else if (u.status === "EXPIRED" || u.isExpired) {
        expiredPaidCount++;
      }

      // Group paid user by purchase month (YYYY-MM)
      const pTime = u.purchaseDate || u.createdAt;
      let monthKey = "2026-09";
      if (pTime > 0) {
        try {
          monthKey = new Date(pTime).toISOString().slice(0, 7);
        } catch (e) {
          monthKey = "2026-09";
        }
      }

      if (!monthlySalesMap[monthKey]) {
        let displayName = monthKey;
        try {
          const parts = monthKey.split("-");
          const y = parts[0];
          const m = parseInt(parts[1], 10);
          if (m >= 1 && m <= 12) {
            displayName = `${monthNames[m - 1]} ${y}`;
          }
        } catch (e) {}

        monthlySalesMap[monthKey] = {
          monthKey,
          monthName: displayName,
          totalSales: 0,
          orderCount: 0,
          monthlyCount: 0,
          yearlyCount: 0,
          lifetimeCount: 0
        };
      }

      monthlySalesMap[monthKey].totalSales += price;
      monthlySalesMap[monthKey].orderCount += 1;
      if (plan === "MONTHLY") monthlySalesMap[monthKey].monthlyCount++;
      else if (plan === "YEARLY") monthlySalesMap[monthKey].yearlyCount++;
      else if (plan === "LIFETIME") monthlySalesMap[monthKey].lifetimeCount++;
    }

    if (u.createdAt > 0) {
      const dateStr = new Date(u.createdAt).toISOString().slice(0, 10);
      dailyTrends[dateStr] = (dailyTrends[dateStr] || 0) + 1;
    }
  });

  const totalPaid = monthlyCount + yearlyCount + lifetimeCount;
  const conversionRate = totalUsers > 0 ? ((totalPaid / totalUsers) * 100).toFixed(2) : "0.00";
  const monthlySales = Object.values(monthlySalesMap).sort((a, b) => b.monthKey.localeCompare(a.monthKey));

  // If Marketing Role, sanitize and hide all revenue/paid stats
  if (role === "MARKETING") {
    return {
      role: "MARKETING",
      totalLeads: trialCount,
      phoneLeads: phoneLeadsCount,
      contactsSavedTotal: totalContactsSavedByLeads,
      dailyTrends
    };
  }

  return {
    role: "OWNER",
    totalUsers,
    trialCount,
    totalPaid,
    monthlyCount,
    yearlyCount,
    lifetimeCount,
    activePaidCount,
    expiredPaidCount,
    expiringSoonCount,
    grossRevenue,
    mrr,
    conversionRate: `${conversionRate}%`,
    planBreakdown,
    dailyTrends,
    monthlySales
  };
}

// ─────────────────────────────────────────────────────────────────
// Helper: Activate / Extend Plan in Firestore
// ─────────────────────────────────────────────────────────────────
async function activateUserPlanInFirestore(userId, planId, customExpiryDate) {
  const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
  const now = Date.now();
  const normalized = normalizePlanType(planId);

  let expiryDate = 0;
  if (customExpiryDate) {
    expiryDate = parseInt(customExpiryDate, 10);
  } else if (normalized === "MONTHLY") {
    expiryDate = now + 2592000000; // 30 days
  } else if (normalized === "YEARLY") {
    expiryDate = now + 31536000000; // 365 days
  } else if (normalized === "LIFETIME") {
    expiryDate = 0; // Lifetime = 0
  }

  const firestoreUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/users/${userId}?updateMask.fieldPaths=planType&updateMask.fieldPaths=purchaseDate&updateMask.fieldPaths=expiryDate&updateMask.fieldPaths=isActive&updateMask.fieldPaths=paymentProvider&updateMask.fieldPaths=status&updateMask.fieldPaths=lastUpdated`;

  const updatePayload = {
    fields: {
      userId: { stringValue: userId },
      planType: { stringValue: normalized },
      status: { stringValue: "ACTIVE" },
      paymentProvider: { stringValue: "ADMIN_MANUAL" },
      purchaseDate: { integerValue: now.toString() },
      expiryDate: { integerValue: expiryDate.toString() },
      isActive: { booleanValue: true },
      lastUpdated: { integerValue: now.toString() }
    }
  };

  const patchResponse = await fetch(firestoreUrl, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${accessToken}`
    },
    body: JSON.stringify(updatePayload)
  });

  if (!patchResponse.ok) {
    const errorText = await patchResponse.text();
    throw new Error(`Firestore plan activation failed: ${errorText}`);
  }

  return {
    success: true,
    planType: normalized,
    isActive: true,
    expiryDate: expiryDate,
    message: `Plan ${normalized} activated successfully for user ${userId}`
  };
}

// ─────────────────────────────────────────────────────────────────
// Helper: Deactivate Plan in Firestore (Resets to TRIAL)
// ─────────────────────────────────────────────────────────────────
async function deactivateUserPlanInFirestore(userId) {
  const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
  const now = Date.now();

  const firestoreUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/users/${userId}?updateMask.fieldPaths=planType&updateMask.fieldPaths=isActive&updateMask.fieldPaths=status&updateMask.fieldPaths=expiryDate&updateMask.fieldPaths=contactsSaved&updateMask.fieldPaths=trialContactsLimit&updateMask.fieldPaths=lastUpdated`;

  const updatePayload = {
    fields: {
      userId: { stringValue: userId },
      planType: { stringValue: "TRIAL" },
      status: { stringValue: "DEACTIVATED" },
      isActive: { booleanValue: false },
      contactsSaved: { integerValue: "100" },
      trialContactsLimit: { integerValue: "100" },
      expiryDate: { integerValue: "0" },
      lastUpdated: { integerValue: now.toString() }
    }
  };

  const patchResponse = await fetch(firestoreUrl, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${accessToken}`
    },
    body: JSON.stringify(updatePayload)
  });

  if (!patchResponse.ok) {
    const errText = await patchResponse.text();
    throw new Error(`Deactivate failed: ${errText}`);
  }

  return {
    success: true,
    planType: "TRIAL",
    isActive: false,
    message: `Plan deactivated successfully for user ${userId}`
  };
}

// ─────────────────────────────────────────────────────────────────
// Helper: Reset Trial Contacts in Firestore
// ─────────────────────────────────────────────────────────────────
async function resetUserTrialInFirestore(userId) {
  const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
  const now = Date.now();

  const firestoreUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/users/${userId}?updateMask.fieldPaths=contactsSaved&updateMask.fieldPaths=trialContactsLimit&updateMask.fieldPaths=lastUpdated`;

  const updatePayload = {
    fields: {
      contactsSaved: { integerValue: "0" },
      trialContactsLimit: { integerValue: "100" },
      lastUpdated: { integerValue: now.toString() }
    }
  };

  const patchResponse = await fetch(firestoreUrl, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${accessToken}`
    },
    body: JSON.stringify(updatePayload)
  });

  if (!patchResponse.ok) {
    const errText = await patchResponse.text();
    throw new Error(`Reset trial failed: ${errText}`);
  }

  return { success: true, message: `Trial contacts reset to 0/100 for user ${userId}` };
}

// ─────────────────────────────────────────────────────────────────
// Helper: Get User Plan Status for App Sync
// ─────────────────────────────────────────────────────────────────
async function getUserPlanStatusFromFirestore(userId) {
  const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
  const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/users/${userId}`;

  const resp = await fetch(url, {
    headers: { "Authorization": `Bearer ${accessToken}` }
  });

  if (!resp.ok) {
    throw new Error(`Failed to fetch user doc: ${resp.status}`);
  }

  const docData = await resp.json();
  const fields = docData.fields || {};

  const parseVal = (f) => {
    if (!f) return null;
    if (f.stringValue !== undefined) return f.stringValue;
    if (f.booleanValue !== undefined) return f.booleanValue;
    if (f.integerValue !== undefined) return parseInt(f.integerValue, 10);
    return null;
  };

  const rawPlan = parseVal(fields.planType);
  const planType = normalizePlanType(rawPlan);
  const isActive = parseVal(fields.isActive) === true || parseVal(fields.status) === "ACTIVE";
  const expiryDate = parseVal(fields.expiryDate) || 0;

  const now = Date.now();
  let effectiveActive = isActive;
  if (planType !== "LIFETIME" && expiryDate > 0 && now > expiryDate) {
    effectiveActive = false;
  }

  return {
    userId,
    planType: effectiveActive ? planType : "TRIAL",
    isPremium: effectiveActive,
    isActive: effectiveActive,
    expiryDate
  };
}

// ─────────────────────────────────────────────────────────────────
// Helper: Get App Settings from Firestore
// ─────────────────────────────────────────────────────────────────
async function getAppSettingsFromFirestore() {
  const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
  const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/app_settings/config`;

  const resp = await fetch(url, { headers: { "Authorization": `Bearer ${accessToken}` } });
  if (!resp.ok) {
    if (resp.status === 404) return { latest_version: 0, update_action: "SOFT_UPDATE" };
    throw new Error("Failed to fetch app settings");
  }

  const doc = await resp.json();
  const fields = doc.fields || {};
  let lv = 0;
  if (fields.latest_version) {
    lv = fields.latest_version.integerValue ? parseInt(fields.latest_version.integerValue, 10) : 0;
  }
  let ua = "SOFT_UPDATE";
  if (fields.update_action && fields.update_action.stringValue) {
    ua = fields.update_action.stringValue;
  }
  return { latest_version: lv, update_action: ua };
}

// ─────────────────────────────────────────────────────────────────
// Helper: Update App Settings in Firestore
// ─────────────────────────────────────────────────────────────────
async function updateAppSettingsInFirestore(latestVersion, updateAction) {
  const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
  const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/app_settings/config?updateMask.fieldPaths=latest_version&updateMask.fieldPaths=update_action`;

  const payload = {
    fields: {
      latest_version: { integerValue: latestVersion.toString() },
      update_action: { stringValue: updateAction }
    }
  };

  const resp = await fetch(url, {
    method: "PATCH",
    headers: { "Content-Type": "application/json", "Authorization": `Bearer ${accessToken}` },
    body: JSON.stringify(payload)
  });

  if (!resp.ok) {
    const errorText = await resp.text();
    throw new Error(`Failed to update app settings: ${errorText}`);
  }
  return { success: true };
}

// ─────────────────────────────────────────────────────────────────
// Helper: Get Tutorials from Firestore
// ─────────────────────────────────────────────────────────────────
async function getTutorialsFromFirestore() {
  const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
  const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/app_settings/tutorials`;

  const resp = await fetch(url, { headers: { "Authorization": `Bearer ${accessToken}` } });
  if (!resp.ok) {
    if (resp.status === 404) return [];
    throw new Error("Failed to fetch tutorials");
  }

  const doc = await resp.json();
  const fields = doc.fields || {};
  if (!fields.videos || !fields.videos.arrayValue || !fields.videos.arrayValue.values) {
    return [];
  }

  return fields.videos.arrayValue.values.map(val => {
    const mapFields = val.mapValue ? val.mapValue.fields : {};
    const rawVid = mapFields.videoId ? mapFields.videoId.stringValue : "";
    const rawUrl = mapFields.url ? mapFields.url.stringValue : "";
    const videoId = extractYouTubeVideoId(rawVid || rawUrl);
    return {
      videoId: videoId,
      url: rawUrl || (videoId ? `https://youtu.be/${videoId}` : ""),
      language: mapFields.language ? mapFields.language.stringValue : "ENGLISH",
      position: mapFields.position ? parseInt(mapFields.position.integerValue, 10) : 0,
      title: mapFields.title ? mapFields.title.stringValue : ""
    };
  });
}

// ─────────────────────────────────────────────────────────────────
// Helper: Update Tutorials in Firestore (Auto Extracts videoId)
// ─────────────────────────────────────────────────────────────────
async function updateTutorialsInFirestore(tutorialsArray) {
  const accessToken = await getGoogleAccessToken(FIREBASE_SERVICE_ACCOUNT);
  
  const arrayValues = tutorialsArray.map((tut, idx) => {
    const videoId = extractYouTubeVideoId(tut.videoId || tut.url);
    const fullUrl = tut.url || (videoId ? `https://youtu.be/${videoId}` : "");
    const pos = tut.position !== undefined ? parseInt(tut.position, 10) : idx + 1;

    return {
      mapValue: {
        fields: {
          videoId: { stringValue: videoId },
          url: { stringValue: fullUrl },
          language: { stringValue: (tut.language || "ENGLISH").toUpperCase() },
          position: { integerValue: (isNaN(pos) ? idx + 1 : pos).toString() },
          title: { stringValue: tut.title || "" }
        }
      }
    };
  });

  const payload = {
    fields: {
      videos: {
        arrayValue: {
          values: arrayValues
        }
      }
    }
  };
  
  const patchUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/app_settings/tutorials?updateMask.fieldPaths=videos`;
  let resp = await fetch(patchUrl, {
    method: "PATCH",
    headers: { "Content-Type": "application/json", "Authorization": `Bearer ${accessToken}` },
    body: JSON.stringify(payload)
  });

  if (!resp.ok && resp.status === 404) {
    const postUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_SERVICE_ACCOUNT.project_id}/databases/(default)/documents/app_settings?documentId=tutorials`;
    resp = await fetch(postUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${accessToken}` },
      body: JSON.stringify(payload)
    });
  }

  if (!resp.ok) {
    const errorText = await resp.text();
    throw new Error(`Failed to update tutorials: ${errorText}`);
  }
}

// ─────────────────────────────────────────────────────────────────
// Helper: Obtain Google Access Token via OAuth2 RS256 Signed JWT
// ─────────────────────────────────────────────────────────────────
async function getGoogleAccessToken(sa) {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claimSet = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: sa.token_uri,
    exp: now + 3600,
    iat: now
  };

  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedClaimSet = base64UrlEncode(JSON.stringify(claimSet));
  const signatureInput = `${encodedHeader}.${encodedClaimSet}`;

  const pemContents = sa.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s+/g, "");

  const binaryDer = base64Decode(pemContents);

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(signatureInput)
  );

  const jwt = `${signatureInput}.${base64UrlEncode(new Uint8Array(signature))}`;

  const tokenResp = await fetch(sa.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt
    })
  });

  const data = await tokenResp.json();
  if (!tokenResp.ok) {
    throw new Error(`OAuth2 error: ${data.error_description || data.error || "Token exchange failed"}`);
  }
  return data.access_token;
}

function base64UrlEncode(data) {
  let str = "";
  if (typeof data === "string") {
    str = btoa(unescape(encodeURIComponent(data)));
  } else {
    let binary = "";
    const bytes = new Uint8Array(data);
    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    str = btoa(binary);
  }
  return str.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function base64Decode(base64Str) {
  const binaryStr = atob(base64Str);
  const bytes = new Uint8Array(binaryStr.length);
  for (let i = 0; i < binaryStr.length; i++) {
    bytes[i] = binaryStr.charCodeAt(i);
  }
  return bytes;
}

function jsonResponse(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      "Content-Type": "application/json",
      ...headers
    }
  });
}

// ─────────────────────────────────────────────────────────────────
// HTML Dashboard Template (Role-Based SPA for Owner & Marketing Team)
// ─────────────────────────────────────────────────────────────────
function renderAdminHTML() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
  <title>Auto Contact Saver — Mission Control & Leads Portal</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  <style>
    :root {
      --bg-main: #07090E;
      --bg-surface: #0E131F;
      --bg-card: #141A29;
      --bg-card-hover: #1A2236;
      --border-color: #1F293D;
      --border-focus: #3B82F6;
      
      --accent-green: #10B981;
      --accent-green-glow: rgba(16, 185, 129, 0.2);
      --accent-blue: #3B82F6;
      --accent-purple: #8B5CF6;
      --accent-gold: #F59E0B;
      --accent-red: #EF4444;
      
      --text-primary: #F8FAFC;
      --text-secondary: #94A3B8;
      --text-muted: #64748B;
      --radius-sm: 8px;
      --radius-md: 12px;
      --radius-lg: 16px;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
      -webkit-tap-highlight-color: transparent;
    }

    body {
      background: radial-gradient(circle at 10% 10%, #0F172A 0%, var(--bg-main) 100%);
      color: var(--text-primary);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
      padding-bottom: env(safe-area-inset-bottom, 20px);
    }

    /* Scrollbars */
    ::-webkit-scrollbar { width: 6px; height: 6px; }
    ::-webkit-scrollbar-track { background: var(--bg-main); }
    ::-webkit-scrollbar-thumb { background: #1E293B; border-radius: 4px; }

    /* Login Screen Container */
    #loginScreen {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 20px;
    }

    .login-card {
      width: 100%;
      max-width: 420px;
      background: rgba(14, 19, 31, 0.9);
      backdrop-filter: blur(20px);
      border: 1px solid var(--border-color);
      border-radius: 20px;
      padding: 32px 28px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.8);
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .login-header {
      text-align: center;
    }
    .login-logo {
      width: 52px;
      height: 52px;
      background: linear-gradient(135deg, #10B981, #059669);
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 26px;
      margin: 0 auto 12px;
      box-shadow: 0 0 24px var(--accent-green-glow);
    }
    .login-header h2 {
      font-size: 20px;
      font-weight: 800;
      letter-spacing: -0.3px;
      margin-bottom: 4px;
    }
    .login-header p {
      font-size: 12.5px;
      color: var(--text-muted);
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .form-label {
      font-size: 11.5px;
      font-weight: 700;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.3px;
    }
    .input-field {
      width: 100%;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      color: var(--text-primary);
      padding: 12px 14px;
      border-radius: var(--radius-md);
      font-size: 14px;
      outline: none;
      transition: all 0.2s;
    }
    .input-field:focus {
      border-color: var(--border-focus);
      box-shadow: 0 0 0 3px rgba(59,130,246,0.15);
    }

    .quick-roles {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      margin-top: 4px;
    }
    .quick-role-btn {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-sm);
      padding: 8px 10px;
      font-size: 11.5px;
      color: var(--text-secondary);
      cursor: pointer;
      text-align: center;
      transition: all 0.15s;
    }
    .quick-role-btn:active {
      transform: scale(0.97);
      background: var(--bg-card-hover);
    }

    /* Main Dashboard Layout (Shown after login) */
    #dashboardScreen {
      display: none;
      flex-direction: column;
      min-height: 100vh;
    }

    /* Top Header */
    header {
      background: rgba(14, 19, 31, 0.92);
      backdrop-filter: blur(16px);
      border-bottom: 1px solid var(--border-color);
      position: sticky;
      top: 0;
      z-index: 40;
      padding: 12px 18px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
    }

    .brand-group {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }

    .brand-icon {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #10B981, #059669);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      flex-shrink: 0;
      box-shadow: 0 0 16px var(--accent-green-glow);
    }

    .brand-text {
      min-width: 0;
    }

    .brand-text h1 {
      font-size: 15px;
      font-weight: 800;
      letter-spacing: -0.2px;
      display: flex;
      align-items: center;
      gap: 6px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .badge-role {
      font-size: 9.5px;
      font-weight: 800;
      text-transform: uppercase;
      padding: 2px 7px;
      border-radius: 12px;
      flex-shrink: 0;
    }
    .badge-role-owner {
      background: rgba(245, 158, 11, 0.15);
      color: #FBBF24;
      border: 1px solid rgba(245, 158, 11, 0.3);
    }
    .badge-role-marketing {
      background: rgba(139, 92, 246, 0.15);
      color: #A78BFA;
      border: 1px solid rgba(139, 92, 246, 0.3);
    }

    .brand-text p {
      font-size: 11px;
      color: var(--text-muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }

    .cmd-k-trigger {
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      padding: 7px 12px;
      border-radius: var(--radius-md);
      color: var(--text-secondary);
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .cmd-k-trigger:active { transform: scale(0.97); }

    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 8px 14px;
      border-radius: var(--radius-md);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      border: none;
      outline: none;
      min-height: 36px;
    }
    .btn:active { transform: scale(0.96); }

    .btn-primary {
      background: linear-gradient(135deg, #10B981, #059669);
      color: white;
      box-shadow: 0 4px 12px var(--accent-green-glow);
    }

    .btn-secondary {
      background: var(--bg-card);
      color: var(--text-primary);
      border: 1px solid var(--border-color);
    }

    .btn-icon {
      width: 36px;
      height: 36px;
      padding: 0;
      border-radius: 10px;
    }

    /* Main Container */
    main {
      flex: 1;
      max-width: 1500px;
      width: 100%;
      margin: 0 auto;
      padding: 16px 16px 40px;
    }

    /* Nav Tabs - Horizontal Touch Scroll for Mobile */
    .nav-tabs-wrapper {
      width: 100%;
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      scrollbar-width: none;
      margin-bottom: 18px;
      padding-bottom: 2px;
    }
    .nav-tabs-wrapper::-webkit-scrollbar { display: none; }

    .nav-tabs {
      display: flex;
      align-items: center;
      gap: 6px;
      background: var(--bg-surface);
      padding: 5px;
      border-radius: var(--radius-lg);
      border: 1px solid var(--border-color);
      width: max-content;
      min-width: 100%;
    }

    .tab-btn {
      padding: 8px 16px;
      border-radius: var(--radius-md);
      font-size: 12.5px;
      font-weight: 600;
      color: var(--text-secondary);
      background: transparent;
      border: none;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      gap: 7px;
      white-space: nowrap;
      flex: 1;
      justify-content: center;
    }
    .tab-btn.active {
      background: var(--bg-card);
      color: var(--text-primary);
      border: 1px solid rgba(255,255,255,0.08);
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    }

    .tab-pane {
      display: none;
    }
    .tab-pane.active {
      display: block;
      animation: fadeIn 0.2s ease-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(4px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* KPI Metrics Cards */
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 12px;
      margin-bottom: 14px;
    }
    @media (max-width: 600px) {
      .kpi-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 10px;
      }
    }

    .plan-breakdown-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 10px;
      margin-bottom: 18px;
    }
    @media (max-width: 600px) {
      .plan-breakdown-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    .kpi-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 16px;
      position: relative;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .kpi-card::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0; height: 3px;
      background: linear-gradient(90deg, transparent, var(--kpi-accent, var(--accent-green)), transparent);
    }

    .plan-mini-card {
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 12px 14px;
      cursor: pointer;
      transition: all 0.15s;
    }
    .plan-mini-card:hover, .plan-mini-card:active {
      background: var(--bg-card-hover);
      border-color: #3B82F6;
    }

    .kpi-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }
    .kpi-title {
      font-size: 11px;
      font-weight: 700;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.4px;
    }
    .kpi-icon-badge {
      width: 28px;
      height: 28px;
      border-radius: 7px;
      background: rgba(255,255,255,0.04);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
    }
    .kpi-value {
      font-size: 22px;
      font-weight: 800;
      letter-spacing: -0.4px;
      margin-bottom: 4px;
    }
    @media (min-width: 768px) {
      .kpi-value { font-size: 26px; }
    }
    .kpi-footer {
      font-size: 11px;
      color: var(--text-muted);
      display: flex;
      align-items: center;
      gap: 5px;
      flex-wrap: wrap;
    }
    .kpi-tag {
      padding: 2px 5px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 700;
    }
    .tag-green { background: rgba(16,185,129,0.15); color: #34D399; }
    .tag-blue { background: rgba(59,130,246,0.15); color: #60A5FA; }
    .tag-gold { background: rgba(245,158,11,0.15); color: #FBBF24; }
    .tag-purple { background: rgba(139,92,246,0.15); color: #A78BFA; }

    /* Filter Pills & Quick Toggles */
    .filter-pills {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 12px;
      align-items: center;
    }
    .pill-btn {
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 700;
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      color: var(--text-secondary);
      cursor: pointer;
      transition: all 0.15s ease;
      display: inline-flex;
      align-items: center;
      gap: 5px;
    }
    .pill-btn:hover {
      background: var(--bg-card-hover);
      color: var(--text-primary);
      border-color: #475569;
    }
    .pill-btn.active {
      background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(139, 92, 246, 0.2));
      border-color: #3B82F6;
      color: #F8FAFC;
      box-shadow: 0 0 10px rgba(59, 130, 246, 0.25);
    }
    .pill-btn.pill-paid.active {
      background: linear-gradient(135deg, rgba(245, 158, 11, 0.25), rgba(217, 119, 6, 0.2));
      border-color: #F59E0B;
      color: #FBBF24;
      box-shadow: 0 0 12px rgba(245, 158, 11, 0.35);
    }
    .pill-btn.pill-monthly.active {
      background: rgba(16, 185, 129, 0.2);
      border-color: #10B981;
      color: #34D399;
    }
    .pill-btn.pill-yearly.active {
      background: rgba(139, 92, 246, 0.2);
      border-color: #8B5CF6;
      color: #A78BFA;
    }

    /* Month-Wise Sales Cards */
    .month-sales-card {
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 14px 16px;
      transition: all 0.2s ease;
      position: relative;
    }
    .month-sales-card:hover {
      border-color: #3B82F6;
      transform: translateY(-2px);
      box-shadow: 0 4px 14px rgba(0,0,0,0.3);
    }

    /* Charts Layout */
    .charts-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 16px;
      margin-bottom: 20px;
    }
    @media (min-width: 900px) {
      .charts-grid { grid-template-columns: 3fr 2fr; }
    }

    .chart-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 16px;
    }
    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    .chart-header h3 {
      font-size: 14px;
      font-weight: 700;
    }

    /* CRM Panel */
    .crm-panel {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      overflow: hidden;
    }

    .filter-bar {
      padding: 12px 14px;
      border-bottom: 1px solid var(--border-color);
      display: flex;
      flex-direction: column;
      gap: 10px;
      background: rgba(14, 19, 31, 0.4);
    }
    @media (min-width: 768px) {
      .filter-bar {
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
      }
    }

    .search-input-box {
      position: relative;
      width: 100%;
    }
    .search-input-box svg {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-muted);
      pointer-events: none;
    }
    .search-input {
      width: 100%;
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      color: var(--text-primary);
      padding: 9px 12px 9px 36px;
      border-radius: var(--radius-md);
      font-size: 13px;
      outline: none;
      min-height: 38px;
    }
    .search-input:focus {
      border-color: var(--border-focus);
      box-shadow: 0 0 0 3px rgba(59,130,246,0.15);
    }

    .filter-group {
      display: flex;
      align-items: center;
      gap: 6px;
      overflow-x: auto;
      padding-bottom: 2px;
      scrollbar-width: none;
      width: 100%;
    }
    .filter-group::-webkit-scrollbar { display: none; }

    .select-dropdown {
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      color: var(--text-primary);
      padding: 8px 12px;
      border-radius: var(--radius-md);
      font-size: 12px;
      outline: none;
      cursor: pointer;
      min-height: 36px;
      flex-shrink: 0;
    }

    /* View Switcher (Cards vs Table) */
    .view-toggle-btn {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      padding: 6px 10px;
      border-radius: 6px;
      background: var(--bg-surface);
      color: var(--text-secondary);
      border: 1px solid var(--border-color);
      cursor: pointer;
      flex-shrink: 0;
    }
    .view-toggle-btn.active {
      background: #1E293B;
      color: white;
      border-color: #3B82F6;
    }

    /* Mobile User Cards View */
    .mobile-cards-container {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding: 12px;
    }
    @media (min-width: 769px) {
      .mobile-cards-container.hide-on-desktop { display: none; }
    }

    .user-mobile-card {
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 14px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      transition: background 0.15s;
    }
    .user-mobile-card:active {
      background: var(--bg-card-hover);
    }

    .user-card-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
    }
    .user-card-profile {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }
    .user-card-avatar {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: linear-gradient(135deg, #3B82F6, #1D4ED8);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 14px;
      color: white;
      flex-shrink: 0;
    }
    .user-card-info {
      min-width: 0;
    }
    .user-card-info .name {
      font-weight: 700;
      font-size: 13.5px;
      color: var(--text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .user-card-info .email {
      font-size: 11.5px;
      color: var(--text-muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .user-card-badges {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
    }

    .user-card-bottom {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-top: 8px;
      border-top: 1px solid rgba(255,255,255,0.04);
      font-size: 12px;
      color: var(--text-muted);
    }

    /* Desktop Table View */
    .table-container {
      width: 100%;
      overflow-x: auto;
    }
    @media (max-width: 768px) {
      .table-container.hide-on-mobile { display: none; }
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;
      text-align: left;
    }
    th {
      background: #0E1422;
      color: var(--text-secondary);
      font-weight: 700;
      font-size: 11px;
      text-transform: uppercase;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color);
      white-space: nowrap;
    }
    td {
      padding: 12px 16px;
      border-bottom: 1px solid rgba(31, 41, 61, 0.6);
      vertical-align: middle;
      white-space: nowrap;
    }

    /* Badges */
    .plan-badge {
      display: inline-flex;
      align-items: center;
      font-size: 10.5px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 6px;
      text-transform: uppercase;
      letter-spacing: 0.3px;
    }
    .badge-lifetime { background: rgba(245,158,11,0.15); color: #F59E0B; border: 1px solid rgba(245,158,11,0.3); }
    .badge-yearly { background: rgba(139,92,246,0.15); color: #A78BFA; border: 1px solid rgba(139,92,246,0.3); }
    .badge-monthly { background: rgba(16,185,129,0.15); color: #10B981; border: 1px solid rgba(16,185,129,0.3); }
    .badge-trial { background: rgba(148,163,184,0.12); color: #94A3B8; border: 1px solid rgba(148,163,184,0.2); }

    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 10.5px;
      font-weight: 600;
      padding: 2px 7px;
      border-radius: 12px;
    }
    .status-active { background: rgba(16,185,129,0.12); color: #34D399; }
    .status-expiring { background: rgba(245,158,11,0.15); color: #FBBF24; }
    .status-expired { background: rgba(239,68,68,0.15); color: #F87171; }
    .status-trial { background: rgba(100,116,139,0.15); color: #94A3B8; }

    /* Action Buttons */
    .btn-table {
      padding: 6px 10px;
      font-size: 12px;
      border-radius: 6px;
      border: 1px solid var(--border-color);
      background: var(--bg-surface);
      color: var(--text-secondary);
      cursor: pointer;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      min-height: 32px;
    }
    .btn-wa {
      color: #25D366;
      border-color: rgba(37,211,102,0.3);
      background: rgba(37,211,102,0.08);
      font-weight: 700;
    }

    /* Pagination */
    .pagination-bar {
      padding: 12px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-top: 1px solid var(--border-color);
      font-size: 11.5px;
      color: var(--text-muted);
      background: rgba(14, 19, 31, 0.4);
      flex-wrap: wrap;
      gap: 8px;
    }

    /* Customer Slide-Over Drawer (Mobile Full Width) */
    .drawer-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.75);
      backdrop-filter: blur(4px);
      z-index: 99;
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.25s ease;
    }
    .drawer-overlay.open {
      opacity: 1;
      pointer-events: auto;
    }

    .drawer {
      position: fixed;
      top: 0; right: 0; bottom: 0;
      width: 100%;
      max-width: 460px;
      background: var(--bg-surface);
      border-left: 1px solid var(--border-color);
      z-index: 100;
      transform: translateX(100%);
      transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      display: flex;
      flex-direction: column;
      box-shadow: -10px 0 40px rgba(0,0,0,0.7);
    }
    @media (max-width: 500px) {
      .drawer { max-width: 100%; border-left: none; }
    }
    .drawer-overlay.open .drawer {
      transform: translateX(0);
    }

    .drawer-header {
      padding: 16px 20px;
      border-bottom: 1px solid var(--border-color);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .drawer-body {
      padding: 18px;
      overflow-y: auto;
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .drawer-profile-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 18px;
      text-align: center;
    }
    .drawer-avatar {
      width: 56px;
      height: 56px;
      border-radius: 16px;
      background: linear-gradient(135deg, #10B981, #059669);
      margin: 0 auto 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      font-weight: 800;
      color: white;
    }

    .meta-box {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 14px;
    }
    .meta-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 7px 0;
      border-bottom: 1px solid rgba(255,255,255,0.04);
      font-size: 12.5px;
    }
    .meta-row:last-child { border-bottom: none; }
    .meta-label { color: var(--text-muted); }
    .meta-value { font-weight: 600; color: var(--text-primary); text-align: right; max-width: 60%; word-break: break-all; }

    /* Export Hub Modal */
    .export-modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.8);
      backdrop-filter: blur(6px);
      z-index: 110;
      display: none;
      align-items: center;
      justify-content: center;
      padding: 18px;
    }
    .export-modal-overlay.open { display: flex; }
    .export-modal {
      width: 100%;
      max-width: 520px;
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      box-shadow: 0 20px 60px rgba(0,0,0,0.8);
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    .export-modal-header {
      padding: 18px 20px;
      border-bottom: 1px solid var(--border-color);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .export-modal-body {
      padding: 18px 20px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .export-option-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 14px 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      cursor: pointer;
      transition: all 0.15s;
    }
    .export-option-card:hover, .export-option-card:active {
      background: var(--bg-card-hover);
      border-color: #3B82F6;
      transform: translateY(-1px);
    }
    .export-option-info {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .export-icon {
      font-size: 20px;
    }

    /* Command Palette Modal (Ctrl + K) */
    .cmd-palette-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.8);
      backdrop-filter: blur(6px);
      z-index: 110;
      display: none;
      align-items: flex-start;
      justify-content: center;
      padding: 40px 14px;
    }
    .cmd-palette-overlay.open { display: flex; }
    .cmd-palette {
      width: 100%;
      max-width: 520px;
      background: var(--bg-surface);
      border: 1px solid #334155;
      border-radius: var(--radius-lg);
      box-shadow: 0 20px 50px rgba(0,0,0,0.8);
      overflow: hidden;
    }
    .cmd-search-input {
      width: 100%;
      background: transparent;
      border: none;
      border-bottom: 1px solid var(--border-color);
      color: white;
      padding: 16px;
      font-size: 15px;
      outline: none;
    }
    .cmd-results {
      max-height: 320px;
      overflow-y: auto;
      padding: 6px;
    }
    .cmd-item {
      padding: 10px 12px;
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: space-between;
      cursor: pointer;
    }
    .cmd-item:active, .cmd-item:hover { background: var(--bg-card-hover); }

    /* Toast */
    .toast-container {
      position: fixed;
      bottom: 20px;
      right: 16px;
      left: 16px;
      max-width: 380px;
      margin: 0 auto;
      z-index: 120;
      display: flex;
      flex-direction: column;
      gap: 8px;
      pointer-events: none;
    }
    .toast {
      background: #1E293B;
      border: 1px solid #334155;
      color: white;
      padding: 11px 16px;
      border-radius: var(--radius-md);
      font-size: 12.5px;
      box-shadow: 0 10px 30px rgba(0,0,0,0.6);
      pointer-events: auto;
    }

    /* Strict Role-Based Visibility Rules (Guaranteed by CSS !important) */
    body.role-marketing .owner-only {
      display: none !important;
    }
    body.role-owner .marketing-only {
      display: none !important;
    }
    body.role-login #dashboardScreen {
      display: none !important;
    }
    body.role-owner #loginScreen,
    body.role-marketing #loginScreen {
      display: none !important;
    }
  </style>
</head>
<body class="role-login">

  <!-- 1. LOGIN SCREEN -->
  <div id="loginScreen">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">⚡</div>
        <h2>Auto Contact Saver</h2>
        <p>Sign in to your account</p>
      </div>

      <form id="loginForm" onsubmit="event.preventDefault(); doLogin();" style="display:flex; flex-direction:column; gap:16px;">
        <div class="form-group">
          <label class="form-label">Email Address</label>
          <input type="email" id="loginEmail" class="input-field" placeholder="Enter your email" required autocomplete="email">
        </div>

        <div class="form-group">
          <label class="form-label">Security PIN</label>
          <input type="password" id="loginPin" class="input-field" placeholder="Enter PIN" inputmode="numeric" required autocomplete="current-password">
        </div>

        <button type="submit" id="btnLoginSubmit" class="btn btn-primary" style="padding:12px; font-size:14px; font-weight:700; width:100%;">
          🔐 Login to Dashboard
        </button>
      </form>

      <div style="border-top: 1px solid var(--border-color); padding-top: 12px; text-align:center;">
        <div style="font-size:11.5px; color:var(--text-muted);">
          🔒 Auto Contact Saver Management Portal
        </div>
      </div>
    </div>
  </div>

  <!-- 2. MAIN DASHBOARD (Visible After Auth) -->
  <div id="dashboardScreen">
    <!-- Top Navigation Header -->
    <header>
      <div class="brand-group">
        <div class="brand-icon">⚡</div>
        <div class="brand-text">
          <h1>
            Auto Contact Saver 
            <span id="headerRoleBadge" class="badge-role badge-role-owner">Owner</span>
          </h1>
          <p><span style="color:#10B981;">●</span> <span id="syncText">Connecting...</span></p>
        </div>
      </div>

      <div class="header-actions">
        <!-- Instant Role Switcher Button (Owner Only - Never shown to Marketing) -->
        <button id="btnSwitchRole" class="btn btn-secondary owner-only" onclick="quickSwitchRole()" style="font-size:11.5px; font-weight:700; padding:6px 12px; border-color:rgba(139,92,246,0.4); background:rgba(139,92,246,0.12); color:#C4B5FD; white-space:nowrap;">
          <span>🎯 Switch to Marketing View</span>
        </button>

        <div class="cmd-k-trigger" onclick="openCommandPalette()" title="Search users">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <span style="display:none;" id="desktopSearchHint">Search</span>
        </div>

        <button class="btn btn-secondary btn-icon" onclick="loadDashboardData(true)" title="Refresh">
          <svg id="refreshIcon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg>
        </button>

        <button class="btn btn-primary" onclick="openExportModal()" style="padding: 7px 12px;" title="Export CSV Hub">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          <span>Export CSV</span>
        </button>

        <button class="btn btn-secondary btn-icon" onclick="handleLogout()" title="Logout" style="color:var(--accent-red);">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
        </button>
      </div>
    </header>

    <!-- Main Body Content -->
    <main>
      <!-- Nav Tabs - Horizontal Touch Scroll for Mobile -->
      <div class="nav-tabs-wrapper">
        <div class="nav-tabs">
          <button class="tab-btn active" id="tabBtnAnalytics" onclick="switchTab('analytics')">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
            <span id="tabAnalyticsTitle">Analytics & Revenue</span>
          </button>
          
          <button class="tab-btn" id="tabBtnCrm" onclick="switchTab('crm')">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle></svg>
            <span id="tabCrmTitle">Users CRM (<span id="tabUserCount">...</span>)</span>
          </button>
          
          <button class="tab-btn owner-only" id="tabBtnConfig" onclick="switchTab('config')">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
            <span>App Config</span>
          </button>
          
          <button class="tab-btn owner-only" id="tabBtnTutorials" onclick="switchTab('tutorials')">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"></polygon><rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect></svg>
            <span>Tutorials</span>
          </button>
        </div>
      </div>

      <!-- TAB 1: Analytics & Revenue (Dynamic for Owner vs Marketing) -->
      <div id="tab-analytics" class="tab-pane active">
        <!-- Marketing Action Banner (Marketing Only) -->
        <div class="marketing-only" id="marketingLeadsBanner" style="margin-bottom:16px; background:linear-gradient(135deg, rgba(139,92,246,0.15), rgba(59,130,246,0.1)); border:1px solid rgba(139,92,246,0.35); border-radius:var(--radius-lg); padding:16px 20px; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
          <div>
            <div style="font-weight:800; font-size:15px; color:#A78BFA; display:flex; align-items:center; gap:8px;">
              <span>🎯</span> Marketing Lead Pipeline: <span id="mLeadCountBadge" style="color:#F8FAFC;">Free Leads Ready</span>
            </div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">
              All registered free trial users. Pitch them our special ₹199/month or ₹999/year unlimited saving plans on WhatsApp!
            </div>
          </div>
          <button class="btn btn-primary" onclick="switchTab('crm')" style="padding:10px 18px; font-weight:700; background:linear-gradient(135deg, #8B5CF6, #6D28D9); box-shadow:0 4px 14px rgba(139,92,246,0.3);">
            👉 Open Free Leads CRM &amp; Pitch Now
          </button>
        </div>

        <!-- Main KPI Metric Cards Grid -->
        <div class="kpi-grid" id="kpiGrid">
          <!-- Dynamic KPI Cards injected via JS -->
        </div>

        <!-- Plan-Wise Analytics Breakdown Cards Row (Owner Only) -->
        <div class="plan-breakdown-grid owner-only" id="ownerPlanBreakdownGrid">
          <!-- Monthly Card -->
          <div class="plan-mini-card" onclick="filterByPlan('MONTHLY')">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
              <span style="font-size:11px; font-weight:700; color:#10B981; text-transform:uppercase;">📅 Monthly (₹199)</span>
              <span class="kpi-tag tag-green" id="kpiMonthlyCount">0 Users</span>
            </div>
            <div style="font-size:16px; font-weight:800;" id="kpiMonthlyRevenue">₹0</div>
            <div style="font-size:10.5px; color:var(--text-muted); margin-top:2px;">30 Days Subscriptions</div>
          </div>

          <!-- Yearly Card -->
          <div class="plan-mini-card" onclick="filterByPlan('YEARLY')">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
              <span style="font-size:11px; font-weight:700; color:#A78BFA; text-transform:uppercase;">📆 Yearly (₹999)</span>
              <span class="kpi-tag tag-purple" id="kpiYearlyCount">0 Users</span>
            </div>
            <div style="font-size:16px; font-weight:800;" id="kpiYearlyRevenue">₹0</div>
            <div style="font-size:10.5px; color:var(--text-muted); margin-top:2px;">365 Days Subscriptions</div>
          </div>

          <!-- Lifetime Card -->
          <div class="plan-mini-card" onclick="filterByPlan('LIFETIME')">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
              <span style="font-size:11px; font-weight:700; color:#F59E0B; text-transform:uppercase;">⭐ Lifetime (₹1999)</span>
              <span class="kpi-tag tag-gold" id="kpiLifetimeCount">0 Users</span>
            </div>
            <div style="font-size:16px; font-weight:800;" id="kpiLifetimeRevenue">₹0</div>
            <div style="font-size:10.5px; color:var(--text-muted); margin-top:2px;">Permanent Members</div>
          </div>

          <!-- Trial Card -->
          <div class="plan-mini-card" onclick="filterByPlan('TRIAL')">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
              <span style="font-size:11px; font-weight:700; color:#94A3B8; text-transform:uppercase;">⚪ Free Trial</span>
              <span class="kpi-tag" style="background:rgba(148,163,184,0.15); color:#94A3B8;" id="kpiTrialCount">0 Users</span>
            </div>
            <div style="font-size:16px; font-weight:800;">100 Contacts</div>
            <div style="font-size:10.5px; color:var(--text-muted); margin-top:2px;">Potential Upgrade Leads</div>
          </div>
        </div>

        <!-- Charts Section -->
        <div class="charts-grid">
          <div class="chart-card">
            <div class="chart-header">
              <h3 id="growthChartTitle">📈 User Signups & Lead Activity</h3>
            </div>
            <div style="height: 220px; position: relative;">
              <canvas id="growthChart"></canvas>
            </div>
          </div>

          <div class="chart-card" id="donutChartCard">
            <div class="chart-header">
              <h3 id="donutChartTitle">🥧 Plan & Lead Distribution</h3>
            </div>
            <div style="height: 220px; position: relative; display:flex; align-items:center; justify-content:center;">
              <canvas id="planDonutChart"></canvas>
            </div>
          </div>
        </div>

        <!-- Month-Wise Sales Breakdown (Owner Only) -->
        <div class="owner-only" id="ownerMonthlySalesSection" style="margin-top: 24px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; flex-wrap:wrap; gap:10px;">
            <div>
              <div style="font-size:16px; font-weight:800; display:flex; align-items:center; gap:8px;">
                <span>📅</span> Month-Wise Sales Revenue &amp; Orders (हर महीने की कुल सेल)
              </div>
              <div style="font-size:12px; color:var(--text-muted); margin-top:2px;">
                Monthly revenue collections, customer orders, and plan distribution by month
              </div>
            </div>
            <div id="monthlyTotalBadge" class="kpi-tag tag-green" style="font-size:12px; font-weight:700; padding:6px 14px;">
              Total: ₹0
            </div>
          </div>

          <!-- Monthly Cards Grid -->
          <div id="monthlySalesCardsGrid" style="display:grid; grid-template-columns:repeat(auto-fit, minmax(260px, 1fr)); gap:12px; margin-bottom:16px;">
            <!-- Loaded dynamically -->
          </div>

          <!-- Monthly Sales Detailed Table -->
          <div class="table-container" style="padding:16px; margin-bottom: 24px; border-radius:var(--radius-lg); background:var(--bg-card); border:1px solid var(--border-color);">
            <table style="width:100%;">
              <thead>
                <tr>
                  <th>Month (महीना)</th>
                  <th>Total Sales (सेल)</th>
                  <th>Orders (ग्राहक)</th>
                  <th>Monthly (₹199)</th>
                  <th>Yearly (₹999)</th>
                  <th>Lifetime (₹1999)</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody id="monthlySalesTableBody">
                <tr><td colspan="7" style="text-align:center; padding:20px; color:var(--text-muted);">Loading monthly sales...</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Recent Paid Subscribers Quick List on Analytics Tab (Owner Only) -->
        <div class="owner-only" id="ownerRecentPaidSection" style="margin-top: 10px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; flex-wrap:wrap; gap:10px;">
            <div>
              <div style="font-size:16px; font-weight:800; display:flex; align-items:center; gap:8px;">
                <span>👑</span> Recent Paid Customers (हाल ही के प्रीमियम ग्राहक)
              </div>
              <div style="font-size:12px; color:var(--text-muted); margin-top:2px;">
                Direct access to active subscriptions and paid subscribers
              </div>
            </div>
            <button class="btn btn-primary" onclick="filterByPlan('PAID')" style="font-size:12px; padding:6px 14px; font-weight:700;">
              👉 View All Paid Users in CRM
            </button>
          </div>

          <div class="table-container" style="padding:16px; border-radius:var(--radius-lg); background:var(--bg-card); border:1px solid var(--border-color);">
            <table style="width:100%;">
              <thead>
                <tr>
                  <th>Customer</th>
                  <th>Phone</th>
                  <th>Plan Type</th>
                  <th>Status &amp; Validity</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody id="recentPaidTableBody">
                <tr><td colspan="5" style="text-align:center; padding:20px; color:var(--text-muted);">Loading paid subscribers...</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- TAB 2: CRM & User Directory -->
      <div id="tab-crm" class="tab-pane">
        <div class="crm-panel">
          <!-- Quick Filter Pills for Owner -->
          <div class="filter-pills owner-only" id="ownerFilterPills" style="padding: 12px 14px 4px 14px;">
            <button class="pill-btn active" id="pillAll" onclick="setQuickFilter('ALL')">👥 All Users (<span id="pillCountAll">0</span>)</button>
            <button class="pill-btn pill-paid" id="pillPaid" onclick="setQuickFilter('PAID')">👑 All Paid (<span id="pillCountPaid">0</span>)</button>
            <button class="pill-btn pill-monthly" id="pillMonthly" onclick="setQuickFilter('MONTHLY')">📅 Monthly (<span id="pillCountMonthly">0</span>)</button>
            <button class="pill-btn pill-yearly" id="pillYearly" onclick="setQuickFilter('YEARLY')">📆 Yearly (<span id="pillCountYearly">0</span>)</button>
            <button class="pill-btn pill-lifetime" id="pillLifetime" onclick="setQuickFilter('LIFETIME')">⭐ Lifetime (<span id="pillCountLifetime">0</span>)</button>
            <button class="pill-btn" id="pillExpiring" onclick="setQuickFilter('EXPIRING')">⚠️ Expiring (&lt;7d) (<span id="pillCountExpiring">0</span>)</button>
            <button class="pill-btn" id="pillExpired" onclick="setQuickFilter('EXPIRED')">🔴 Expired (<span id="pillCountExpired">0</span>)</button>
            <button class="pill-btn" id="pillTrial" onclick="setQuickFilter('TRIAL')">⚪ Free Trial (<span id="pillCountTrial">0</span>)</button>
          </div>

          <!-- Active Month Filter Banner (Owner Only) -->
          <div id="activeMonthBanner" class="owner-only" style="display:none; align-items:center; justify-content:space-between; margin: 4px 14px 8px 14px; padding: 8px 14px; background: rgba(59, 130, 246, 0.15); border: 1px solid rgba(59, 130, 246, 0.3); border-radius: 8px; font-size: 12px; color: #93C5FD;"></div>

          <div class="filter-bar">
            <div class="search-input-box">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
              <input type="text" id="tableSearchInput" class="search-input" placeholder="Search name, phone, email, UID..." oninput="applyFilters()">
            </div>

            <div class="filter-group">
              <select id="planFilter" class="select-dropdown owner-only" onchange="syncPillFromSelect(); applyFilters();">
                <option value="ALL">All Plans</option>
                <option value="PAID">👑 All Paid Users</option>
                <option value="MONTHLY">📅 Monthly (₹199)</option>
                <option value="YEARLY">📆 Yearly (₹999)</option>
                <option value="LIFETIME">⭐ Lifetime (₹1999)</option>
                <option value="TRIAL">⏳ Free Trial</option>
              </select>

              <select id="statusFilter" class="select-dropdown owner-only" onchange="syncPillFromSelect(); applyFilters();">
                <option value="ALL">All Status</option>
                <option value="ACTIVE">🟢 Active</option>
                <option value="EXPIRING">⚠️ Expiring (&lt;7d)</option>
                <option value="EXPIRED">🔴 Expired</option>
                <option value="TRIAL">⚪ Trial</option>
              </select>

              <select id="marketingLeadFilter" class="select-dropdown marketing-only" onchange="applyFilters()">
                <option value="ALL">All Free Leads</option>
                <option value="WHATSAPP">💬 With Phone (WhatsApp Pitch Ready)</option>
                <option value="ACTIVE_TRIAL">⚡ Active Leads (Contacts Saved &gt; 0)</option>
                <option value="ZERO_CONTACTS">⚪ 0 Contacts Saved (New Signups)</option>
              </select>

              <select id="sortFilter" class="select-dropdown" onchange="applyFilters()">
                <option value="newest">Newest First</option>
                <option value="contactsSaved">Contacts Saved</option>
                <option value="name">Name (A-Z)</option>
              </select>

              <button class="btn btn-secondary" onclick="openExportModal()" style="font-size:11.5px; padding:6px 12px;">📥 Export Hub</button>
              <button class="view-toggle-btn active" id="btnToggleCards" onclick="toggleMobileView('cards')">📱 Cards</button>
              <button class="view-toggle-btn" id="btnToggleTable" onclick="toggleMobileView('table')">💻 Table</button>
            </div>
          </div>

          <!-- Mobile User Cards Container -->
          <div id="mobileCardsContainer" class="mobile-cards-container hide-on-desktop">
            <!-- Loaded dynamically -->
          </div>

          <!-- Desktop Users Table -->
          <div class="table-container hide-on-mobile" id="tableContainer">
            <table>
              <thead>
                <tr id="tableHeaderRow">
                  <th>Customer</th>
                  <th>Phone</th>
                  <th class="owner-only">Plan</th>
                  <th>Status</th>
                  <th>Contacts Saved</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody id="userTableBody">
                <tr><td colspan="6" style="text-align:center; padding: 40px; color:var(--text-muted);">Loading data...</td></tr>
              </tbody>
            </table>
          </div>

          <!-- Pagination Controls -->
          <div class="pagination-bar">
            <div><span id="showingCount">0</span> of <span id="totalFilteredCount">0</span> records</div>
            <div style="display:flex; align-items:center; gap:6px;">
              <button class="btn btn-secondary" id="btnPrevPage" onclick="prevPage()" disabled style="padding:6px 12px;">Prev</button>
              <span id="pageIndicator" style="font-weight:600; font-size:12px;">1</span>
              <button class="btn btn-secondary" id="btnNextPage" onclick="nextPage()" style="padding:6px 12px;">Next</button>
            </div>
          </div>
        </div>
      </div>

      <!-- TAB 3: App Remote Config (Owner Only) -->
      <div id="tab-config" class="tab-pane owner-only">
        <div class="crm-panel" style="padding: 18px; max-width: 600px;">
          <h2 style="font-size:16px; font-weight:800; margin-bottom: 6px;">📱 App Version & Update Controller</h2>
          <p style="font-size:12px; color:var(--text-muted); margin-bottom: 18px;">Control in-app update popups dynamically without uploading a new APK.</p>
          
          <div style="display:flex; flex-direction:column; gap:14px;">
            <div>
              <label style="display:block; font-size:11.5px; font-weight:700; color:var(--text-secondary); margin-bottom:5px;">LATEST APP VERSION CODE</label>
              <input type="number" id="cfgVersionCode" class="search-input" placeholder="e.g. 12">
            </div>

            <div>
              <label style="display:block; font-size:11.5px; font-weight:700; color:var(--text-secondary); margin-bottom:5px;">UPDATE ACTION TYPE</label>
              <select id="cfgUpdateAction" class="select-dropdown" style="width:100%;">
                <option value="SOFT_UPDATE">SOFT_UPDATE (Optional Dismissable)</option>
                <option value="FORCE_UPDATE">FORCE_UPDATE (Mandatory Update)</option>
                <option value="NONE">NONE (No Dialog)</option>
              </select>
            </div>

            <button class="btn btn-primary" onclick="saveAppSettings()" style="margin-top:6px;">Save Settings</button>
          </div>
        </div>
      </div>

      <!-- TAB 4: Tutorial Videos Manager (Owner Only) -->
      <div id="tab-tutorials" class="tab-pane owner-only">
        <div class="crm-panel" style="padding: 18px; max-width: 780px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px;">
            <div>
              <h2 style="font-size:16px; font-weight:800;">📺 YouTube In-App Tutorials</h2>
              <p style="font-size:12px; color:var(--text-muted);">Videos are dynamically updated inside the Android app tutorial screen.</p>
            </div>
            <button class="btn btn-secondary" onclick="addTutorialRow()">+ Add Video</button>
          </div>

          <div id="tutorialListContainer" style="display:flex; flex-direction:column; gap:12px;">
            <!-- Loaded dynamically -->
          </div>

          <button class="btn btn-primary" onclick="saveTutorials()" style="margin-top:16px;">Save All Tutorials</button>
        </div>
      </div>
    </main>
  </div>

  <!-- Slide-Over Single Customer Drawer -->
  <div id="drawerOverlay" class="drawer-overlay" onclick="closeDrawer(event)">
    <div class="drawer" onclick="event.stopPropagation()">
      <div class="drawer-header">
        <h3 style="font-size:15px; font-weight:700;">Customer 360° Profile</h3>
        <button class="btn btn-secondary btn-icon" onclick="closeDrawer()">✕</button>
      </div>

      <div class="drawer-body">
        <div class="drawer-profile-card">
          <div class="drawer-avatar" id="drawerAvatar">U</div>
          <h2 id="drawerName" style="font-size:16px; font-weight:800;">User Name</h2>
          <p id="drawerEmail" style="font-size:12px; color:var(--text-muted); margin-bottom:10px;">user@example.com</p>
          <div id="drawerPlanBadge"></div>
        </div>

        <!-- Quick Direct WhatsApp Link (Primary for Marketing) -->
        <a id="drawerWhatsAppBtn" href="#" target="_blank" class="btn btn-table btn-wa" style="justify-content:center; padding:12px; font-size:13.5px; font-weight:700;">
          💬 Chat on WhatsApp (Pitch Upgrade)
        </a>

        <!-- Metadata Box -->
        <div class="meta-box">
          <div class="meta-row">
            <span class="meta-label">Phone</span>
            <span class="meta-value" id="drawerPhone">—</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">UID</span>
            <span class="meta-value" id="drawerUid" style="font-family:'JetBrains Mono', monospace; font-size:10.5px;">—</span>
          </div>
          <div class="meta-row owner-only">
            <span class="meta-label">Expiry Date</span>
            <span class="meta-value" id="drawerExpiry">—</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">Contacts Saved</span>
            <span class="meta-value" id="drawerContacts">—</span>
          </div>
          <div class="meta-row owner-only">
            <span class="meta-label">Provider</span>
            <span class="meta-value" id="drawerProvider">—</span>
          </div>
        </div>

        <!-- Plan Controller (Owner Only) -->
        <div class="meta-box owner-only" id="drawerPlanControls">
          <h4 style="font-size:12px; font-weight:700; margin-bottom:10px; color:var(--text-secondary);">⚡ CHANGE SUBSCRIPTION</h4>
          
          <div style="display:grid; grid-template-columns: 1fr 1fr; gap:8px; margin-bottom:10px;">
            <button class="btn btn-secondary" onclick="quickActivatePlan('MONTHLY')">+30d Monthly</button>
            <button class="btn btn-secondary" onclick="quickActivatePlan('YEARLY')">+1yr Yearly</button>
            <button class="btn btn-secondary" style="grid-column: span 2; border-color:var(--accent-gold); color:var(--accent-gold);" onclick="quickActivatePlan('LIFETIME')">⭐ Lifetime Forever</button>
          </div>

          <div style="display:flex; gap:6px;">
            <button class="btn btn-secondary" style="flex:1; font-size:11px;" onclick="resetUserTrial()">🔄 Reset Trial</button>
            <button class="btn btn-secondary" style="flex:1; font-size:11px; color:var(--accent-red); border-color:rgba(239,68,68,0.3);" onclick="revokeUserPlan()">🚫 Revoke</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Export Hub Modal -->
  <div id="exportModalOverlay" class="export-modal-overlay" onclick="closeExportModal(event)">
    <div class="export-modal" onclick="event.stopPropagation()">
      <div class="export-modal-header">
        <h3 style="font-size:16px; font-weight:800; display:flex; align-items:center; gap:8px;">
          <span>📥</span> <span>Export CSV Hub</span>
        </h3>
        <button class="btn btn-secondary btn-icon" onclick="closeExportModal()">✕</button>
      </div>

      <div class="export-modal-body" id="exportModalBody">
        <!-- Dynamic Options based on Owner vs Marketing injected via JS -->
      </div>
    </div>
  </div>

  <!-- Command Palette Modal (Ctrl + K) -->
  <div id="cmdPaletteOverlay" class="cmd-palette-overlay" onclick="closeCommandPalette(event)">
    <div class="cmd-palette" onclick="event.stopPropagation()">
      <input type="text" id="cmdInput" class="cmd-search-input" placeholder="Search by name, phone, email..." oninput="handleCmdSearch()">
      <div id="cmdResults" class="cmd-results">
        <div style="padding:16px; text-align:center; color:var(--text-muted); font-size:12px;">Type to search leads...</div>
      </div>
    </div>
  </div>

  <!-- Toast Messages Container -->
  <div id="toastContainer" class="toast-container"></div>

  <!-- Client-Side Dashboard Script -->
  <script>

    let currentUser = null;
    let rawUsers = [];
    let filteredUsers = [];
    let currentPage = 1;
    const pageSize = 40;
    let selectedUser = null;
    let currentViewMode = window.innerWidth <= 768 ? 'cards' : 'table';
    let currentAnalytics = null;
    let selectedQuickFilter = 'ALL';
    let selectedMonthFilter = null;

    let growthChartInstance = null;
    let donutChartInstance = null;

    const PLAN_PRICES = { MONTHLY: 199, YEARLY: 999, LIFETIME: 1999, TRIAL: 0 };

    function normalizeClientPlan(plan) {
      if (!plan) return "TRIAL";
      const p = String(plan).toUpperCase().trim();
      if (p.includes("LIFE") || p.includes("1999")) return "LIFETIME";
      if (p.includes("YEAR") || p.includes("ANNUAL") || p.includes("365") || p.includes("999")) return "YEARLY";
      if (p.includes("MONTH") || p.includes("30") || p.includes("199")) return "MONTHLY";
      return "TRIAL";
    }

    function extractYouTubeIdClient(urlOrId) {
      if (!urlOrId) return "";
      const trimmed = String(urlOrId).trim();
      if (/^[a-zA-Z0-9_-]{11}$/.test(trimmed)) {
        return trimmed;
      }
      const match = trimmed.match(/(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|shorts\\/|live\\/|watch\\?.+&v=))([\\w-]{11})/);
      return match ? match[1] : trimmed;
    }

    function getAuthToken() {
      return localStorage.getItem("acs_auth_token");
    }

    function setAuthToken(token) {
      localStorage.setItem("acs_auth_token", token);
    }

    function removeAuthToken() {
      localStorage.removeItem("acs_auth_token");
    }

    function fillCredentials(email, pin, autoSubmit = false) {
      const eEl = document.getElementById("loginEmail");
      const pEl = document.getElementById("loginPin");
      if (eEl) eEl.value = email;
      if (pEl) pEl.value = pin;
      if (autoSubmit) {
        doLogin(email, pin);
      }
    }

    document.addEventListener("DOMContentLoaded", () => {
      checkExistingSession();

      // Keyboard shortcuts
      document.addEventListener("keydown", (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
          e.preventDefault();
          if (currentUser) openCommandPalette();
        }
        if (e.key === "Escape") {
          closeDrawer();
          closeCommandPalette();
          closeExportModal();
        }
      });

      if (window.innerWidth >= 768) {
        const hint = document.getElementById("desktopSearchHint");
        if (hint) hint.style.display = "inline";
      }
    });

    async function checkExistingSession() {
      const token = getAuthToken();
      if (!token) {
        showLoginScreen();
        return;
      }

      try {
        const resp = await fetch("/api/auth/me", {
          headers: { "Authorization": \`Bearer \${token}\` }
        });
        const data = await resp.json();
        if (data.success && data.user) {
          currentUser = data.user;
          showDashboardScreen();
        } else {
          removeAuthToken();
          showLoginScreen();
        }
      } catch (e) {
        removeAuthToken();
        showLoginScreen();
      }
    }

    function showLoginScreen() {
      document.body.className = "role-login";
      const lEl = document.getElementById("loginScreen");
      const dEl = document.getElementById("dashboardScreen");
      if (lEl) lEl.style.display = "flex";
      if (dEl) dEl.style.display = "none";
    }

    function showDashboardScreen() {
      if (!currentUser) return;
      const isOwner = currentUser.role === "OWNER";

      // 1. Set strict role class on body for CSS isolation
      document.body.className = isOwner ? "role-owner" : "role-marketing";

      const lEl = document.getElementById("loginScreen");
      const dEl = document.getElementById("dashboardScreen");
      if (lEl) lEl.style.display = "none";
      if (dEl) dEl.style.display = "flex";

      // 2. Configure UI based on Role
      const roleBadge = document.getElementById("headerRoleBadge");
      const switchRoleBtn = document.getElementById("btnSwitchRole");
      const aTitle = document.getElementById("tabAnalyticsTitle");
      const cTitle = document.getElementById("tabCrmTitle");

      if (isOwner) {
        if (roleBadge) {
          roleBadge.className = "badge-role badge-role-owner";
          roleBadge.innerText = "👑 Owner";
        }
        if (switchRoleBtn) {
          switchRoleBtn.innerHTML = "<span>🎯 Switch to Marketing View</span>";
          switchRoleBtn.title = "Switch to Marketing View (Free Leads)";
          switchRoleBtn.style.display = "inline-flex";
        }
        if (aTitle) aTitle.innerText = "Analytics & Revenue";
        if (cTitle) cTitle.innerText = "Users CRM";
      } else {
        if (roleBadge) {
          roleBadge.className = "badge-role badge-role-marketing";
          roleBadge.innerText = "🎯 Marketing Team";
        }
        if (switchRoleBtn) {
          switchRoleBtn.style.display = "none";
        }
        if (aTitle) aTitle.innerText = "Leads Overview";
        if (cTitle) cTitle.innerText = "Free Leads CRM";
      }

      // 3. Hide or show Owner-only elements via JS (backed by CSS !important)
      document.querySelectorAll(".owner-only").forEach(el => {
        el.style.display = isOwner ? "" : "none";
      });

      // 4. Hide or show Marketing-only elements
      document.querySelectorAll(".marketing-only").forEach(el => {
        el.style.display = !isOwner ? "" : "none";
      });

      // 5. Initialize responsive view mode (cards on mobile, table on desktop)
      const initialMode = window.innerWidth <= 768 ? 'cards' : 'table';
      toggleMobileView(initialMode);

      // 6. Default Active Tab:
      // For Marketing, go DIRECTLY to 'crm' (Free Leads CRM)
      // For Owner, go to 'analytics'
      if (!isOwner) {
        switchTab('crm');
      } else {
        switchTab('analytics');
      }

      loadDashboardData();
      if (isOwner) {
        loadAppSettings();
        loadTutorials();
      }
    }

    async function doLogin(email, pin) {
      const emailInput = document.getElementById("loginEmail");
      const pinInput = document.getElementById("loginPin");
      const emailVal = (email || (emailInput ? emailInput.value : "")).trim();
      const pinVal = (pin || (pinInput ? pinInput.value : "")).trim();

      if (emailInput && email) emailInput.value = email;
      if (pinInput && pin) pinInput.value = pin;

      const btn = document.getElementById("btnLoginSubmit");
      if (btn) {
        btn.disabled = true;
        btn.innerText = "Verifying...";
      }

      try {
        const resp = await fetch("/api/auth/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: emailVal, pin: pinVal })
        });
        const data = await resp.json();
        if (data.success && data.token) {
          setAuthToken(data.token);
          currentUser = data.user;
          rawUsers = [];
          filteredUsers = [];
          showDashboardScreen();
          showToast("👋 Welcome " + (currentUser ? currentUser.name : "") + "!");
        } else {
          showToast("❌ " + (data.error || "Login failed"));
        }
      } catch (err) {
        showToast("❌ Request failed: " + err.message);
      } finally {
        if (btn) {
          btn.disabled = false;
          btn.innerText = "🔐 Login to Dashboard";
        }
      }
    }

    async function quickSwitchRole() {
      if (!currentUser || currentUser.role !== "OWNER") {
        showToast("❌ Permission denied: Owner access only");
        return;
      }

      showToast("Switching to Marketing View...");
      await doLogin("marketing@autocontactsaver.com", "654321");
    }

    async function handleLoginSubmit(e) {
      if (e && e.preventDefault) e.preventDefault();
      await doLogin();
    }

    function handleLogout() {
      removeAuthToken();
      currentUser = null;
      rawUsers = [];
      filteredUsers = [];
      document.body.className = "role-login";
      showLoginScreen();
      showToast("🔒 Logged out safely");
    }

    function switchTab(tabId) {
      document.querySelectorAll(".tab-btn").forEach(btn => btn.classList.remove("active"));
      document.querySelectorAll(".tab-pane").forEach(pane => pane.classList.remove("active"));

      const activeBtn = Array.from(document.querySelectorAll(".tab-btn")).find(b => b.getAttribute("onclick") && b.getAttribute("onclick").includes(tabId));
      if (activeBtn) activeBtn.classList.add("active");

      const pane = document.getElementById("tab-" + tabId);
      if (pane) pane.classList.add("active");
    }

    function toggleMobileView(mode) {
      currentViewMode = mode;
      const btnCards = document.getElementById("btnToggleCards");
      const btnTable = document.getElementById("btnToggleTable");
      if (btnCards) btnCards.classList.toggle("active", mode === 'cards');
      if (btnTable) btnTable.classList.toggle("active", mode === 'table');

      const cardCont = document.getElementById("mobileCardsContainer");
      const tableCont = document.getElementById("tableContainer");

      if (mode === 'cards') {
        if (cardCont) {
          cardCont.style.display = "flex";
          cardCont.classList.remove("hide-on-desktop");
        }
        if (tableCont) {
          tableCont.style.display = "none";
        }
      } else {
        if (cardCont) {
          cardCont.style.display = "none";
        }
        if (tableCont) {
          tableCont.style.display = "block";
          tableCont.classList.remove("hide-on-mobile");
        }
      }
      renderTable();
    }

    async function loadDashboardData(showToastMsg = false) {
      const token = getAuthToken();
      if (!token) return;

      const refreshIcon = document.getElementById("refreshIcon");
      if (refreshIcon) refreshIcon.style.animation = "spin 1s linear infinite";

      try {
        const [usersResp, analyticsResp] = await Promise.all([
          fetch("/api/users", { headers: { "Authorization": \`Bearer \${token}\` } }),
          fetch("/api/analytics", { headers: { "Authorization": \`Bearer \${token}\` } }).catch(() => null)
        ]);

        const data = await usersResp.json();
        if (!data.success) {
          if (usersResp.status === 401) {
            handleLogout();
            return;
          }
          throw new Error(data.error || "Failed to load data");
        }

        if (analyticsResp && analyticsResp.ok) {
          try {
            const aData = await analyticsResp.json();
            if (aData.success && aData.analytics) {
              currentAnalytics = aData.analytics;
            }
          } catch(e) {}
        }

        rawUsers = (data.users || []).map(u => ({
          ...u,
          planType: normalizeClientPlan(u.planType)
        }));

        const isOwner = currentUser && currentUser.role === "OWNER";
        const countEl = document.getElementById("tabUserCount");
        if (countEl) countEl.innerText = rawUsers.length;

        const syncEl = document.getElementById("syncText");
        if (syncEl) syncEl.innerText = \`\${rawUsers.length} \${!isOwner ? 'Free Leads' : 'Users'}\`;

        const mBadge = document.getElementById("mLeadCountBadge");
        if (mBadge) mBadge.innerText = \`\${rawUsers.length} Free Leads Ready\`;

        // 1. Safely update KPIs, monthly sales, recent paid, pill counts
        try {
          updateAnalyticsKPIs();
        } catch (kpiErr) {
          console.warn("KPI update warning:", kpiErr);
        }

        // 2. Render Table & Cards
        applyFilters();

        // 3. Safely render charts so chart issues NEVER block the leads table
        try {
          renderCharts();
        } catch (chartErr) {
          console.warn("Chart render warning:", chartErr);
        }

        if (showToastMsg) showToast("✅ Live data synced from Firestore");
      } catch (err) {
        console.error("loadDashboardData error:", err);
        showToast("❌ Error: " + err.message);
      } finally {
        if (refreshIcon) refreshIcon.style.animation = "none";
      }
    }

    function computeLocalMonthlySales() {
      var monthNames = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
      ];
      var monthlySalesMap = {};

      rawUsers.forEach(function(u) {
        var plan = normalizeClientPlan(u.planType);
        if (plan === "TRIAL") return;

        var price = u.amountPaid || PLAN_PRICES[plan] || 0;
        var pTime = u.purchaseDate || u.createdAt;
        var monthKey = "2026-09";
        if (pTime > 0) {
          try {
            monthKey = new Date(pTime).toISOString().slice(0, 7);
          } catch (e) {
            monthKey = "2026-09";
          }
        }

        if (!monthlySalesMap[monthKey]) {
          var displayName = monthKey;
          try {
            var parts = monthKey.split("-");
            var y = parts[0];
            var m = parseInt(parts[1], 10);
            if (m >= 1 && m <= 12) {
              displayName = monthNames[m - 1] + " " + y;
            }
          } catch (e) {}

          monthlySalesMap[monthKey] = {
            monthKey: monthKey,
            monthName: displayName,
            totalSales: 0,
            orderCount: 0,
            monthlyCount: 0,
            yearlyCount: 0,
            lifetimeCount: 0
          };
        }

        monthlySalesMap[monthKey].totalSales += price;
        monthlySalesMap[monthKey].orderCount += 1;
        if (plan === "MONTHLY") monthlySalesMap[monthKey].monthlyCount++;
        else if (plan === "YEARLY") monthlySalesMap[monthKey].yearlyCount++;
        else if (plan === "LIFETIME") monthlySalesMap[monthKey].lifetimeCount++;
      });

      return Object.values(monthlySalesMap).sort(function(a, b) {
        return b.monthKey.localeCompare(a.monthKey);
      });
    }

    function renderMonthlySalesSection(monthlySales) {
      var isOwner = currentUser && currentUser.role === "OWNER";
      if (!isOwner) return;

      var salesData = (monthlySales && monthlySales.length > 0) ? monthlySales : computeLocalMonthlySales();
      var grid = document.getElementById("monthlySalesCardsGrid");
      var tbody = document.getElementById("monthlySalesTableBody");
      var totalBadge = document.getElementById("monthlyTotalBadge");

      var grandTotal = 0;
      salesData.forEach(function(m) { grandTotal += (m.totalSales || 0); });
      if (totalBadge) {
        totalBadge.innerText = "Total Sales: ₹" + grandTotal.toLocaleString("en-IN");
      }

      if (salesData.length === 0) {
        if (grid) grid.innerHTML = '<div style="color:var(--text-muted); font-size:12px; padding:12px;">No sales records found.</div>';
        if (tbody) tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:20px; color:var(--text-muted);">No sales records found.</td></tr>';
        return;
      }

      // 1. Render Monthly Cards Grid
      if (grid) {
        grid.innerHTML = salesData.map(function(m) {
          var mRev = (m.monthlyCount || 0) * 199;
          var yRev = (m.yearlyCount || 0) * 999;
          var lRev = (m.lifetimeCount || 0) * 1999;
          return '<div class="month-sales-card">' +
            '<div class="month-card-header">' +
              '<span class="month-title">📅 ' + escapeHtml(m.monthName) + '</span>' +
              '<span class="kpi-tag tag-green">₹' + (m.totalSales || 0).toLocaleString("en-IN") + '</span>' +
            '</div>' +
            '<div style="font-size:11.5px; color:var(--text-muted); margin-bottom:8px;">' +
              'Total Orders: <b style="color:var(--text-primary);">' + (m.orderCount || 0) + ' Customers</b>' +
            '</div>' +
            '<div class="month-plan-pills">' +
              '<span class="month-plan-pill pill-monthly">📅 ' + (m.monthlyCount || 0) + ' Monthly (₹' + mRev.toLocaleString("en-IN") + ')</span>' +
              '<span class="month-plan-pill pill-yearly">📆 ' + (m.yearlyCount || 0) + ' Yearly (₹' + yRev.toLocaleString("en-IN") + ')</span>' +
              '<span class="month-plan-pill pill-lifetime">⭐ ' + (m.lifetimeCount || 0) + ' Lifetime (₹' + lRev.toLocaleString("en-IN") + ')</span>' +
            '</div>' +
            '<button class="btn btn-secondary" data-month="" + m.monthKey + "" onclick="filterByMonth(this.dataset.month)" style="width:100%; font-size:11.5px; padding:6px; margin-top:8px; justify-content:center;">' +
              '🔍 View ' + (m.orderCount || 0) + ' Customers of ' + escapeHtml(m.monthName) +
            '</button>' +
          '</div>';
        }).join("");
      }

      // 2. Render Monthly Sales Table
      if (tbody) {
        tbody.innerHTML = salesData.map(function(m) {
          var mRev = (m.monthlyCount || 0) * 199;
          var yRev = (m.yearlyCount || 0) * 999;
          var lRev = (m.lifetimeCount || 0) * 1999;
          return '<tr>' +
            '<td>' +
              '<div style="font-weight:700; color:var(--text-primary); font-size:13px;">📅 ' + escapeHtml(m.monthName) + '</div>' +
              '<div style="font-size:10.5px; font-family:monospace; color:var(--text-muted);">' + m.monthKey + '</div>' +
            '</td>' +
            '<td>' +
              '<span style="font-weight:800; color:#10B981; font-size:14px;">₹' + (m.totalSales || 0).toLocaleString("en-IN") + '</span>' +
            '</td>' +
            '<td>' +
              '<span style="font-weight:700;">' + (m.orderCount || 0) + '</span> <span style="font-size:11px; color:var(--text-muted);">orders</span>' +
            '</td>' +
            '<td>' +
              '<span class="plan-badge badge-monthly" style="font-size:11px;">' + (m.monthlyCount || 0) + ' (₹' + mRev.toLocaleString("en-IN") + ')</span>' +
            '</td>' +
            '<td>' +
              '<span class="plan-badge badge-yearly" style="font-size:11px;">' + (m.yearlyCount || 0) + ' (₹' + yRev.toLocaleString("en-IN") + ')</span>' +
            '</td>' +
            '<td>' +
              '<span class="plan-badge badge-lifetime" style="font-size:11px;">' + (m.lifetimeCount || 0) + ' (₹' + lRev.toLocaleString("en-IN") + ')</span>' +
            '</td>' +
            '<td>' +
              '<button class="btn btn-secondary" data-month="" + m.monthKey + "" onclick="filterByMonth(this.dataset.month)" style="font-size:11.5px; padding:4px 10px;">' +
                '👉 View Customers' +
              '</button>' +
            '</td>' +
          '</tr>';
        }).join("");
      }
    }

    function renderRecentPaidSubscribers() {
      var isOwner = currentUser && currentUser.role === "OWNER";
      if (!isOwner) return;

      var tbody = document.getElementById("recentPaidTableBody");
      if (!tbody) return;

      var paidUsers = rawUsers
        .filter(function(u) { return normalizeClientPlan(u.planType) !== "TRIAL"; })
        .sort(function(a, b) { return (b.purchaseDate || b.createdAt || 0) - (a.purchaseDate || a.createdAt || 0); })
        .slice(0, 10);

      if (paidUsers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:20px; color:var(--text-muted);">No paid subscribers found.</td></tr>';
        return;
      }

      var now = Date.now();
      tbody.innerHTML = paidUsers.map(function(u) {
        var initials = (u.name || "U").substring(0, 2).toUpperCase();
        var plan = normalizeClientPlan(u.planType);
        var planBadgeClass = "badge-monthly";
        var planLabel = "📅 Monthly (₹199)";
        if (plan === "LIFETIME") { planBadgeClass = "badge-lifetime"; planLabel = "⭐ Lifetime (₹1999)"; }
        else if (plan === "YEARLY") { planBadgeClass = "badge-yearly"; planLabel = "📆 Yearly (₹999)"; }

        var statusHtml = '<span class="status-badge status-active">🟢 Active</span>';
        if (plan === "LIFETIME") {
          statusHtml = '<span class="status-badge status-active">🟢 Lifetime</span>';
        } else if (u.expiryDate > 0) {
          var dl = Math.ceil((u.expiryDate - now) / (1000 * 60 * 60 * 24));
          if (dl <= 0) {
            statusHtml = '<span class="status-badge status-expired">🔴 Expired</span>';
          } else if (dl <= 7) {
            statusHtml = '<span class="status-badge status-expiring">⚠️ ' + dl + 'd left</span>';
          } else {
            statusHtml = '<span class="status-badge status-active">🟢 ' + dl + 'd left</span>';
          }
        }

        var rawPhone = (u.fullPhone || u.mobileNumber || "").replace(/[^0-9]/g, "");
        var waPhone = rawPhone;
        if (waPhone.length === 10) waPhone = "91" + waPhone;
        var waLink = waPhone ? ('https://wa.me/' + waPhone) : "#";

        return '<tr>' +
          '<td>' +
            '<div style="display:flex; align-items:center; gap:10px; cursor:pointer;" data-uid="" + u.userId + "" onclick="openCustomerDrawer(this.dataset.uid)">' +
              '<div class="user-card-avatar" style="width:34px; height:34px; font-size:12px;">' + initials + '</div>' +
              '<div>' +
                '<div style="font-weight:700; color:var(--text-primary); font-size:13px;">' + escapeHtml(u.name || 'App User') + '</div>' +
                '<div style="font-size:11px; color:var(--text-muted);">' + escapeHtml(u.email || 'No email') + '</div>' +
              '</div>' +
            '</div>' +
          '</td>' +
          '<td>' +
            '<span style="font-family:monospace; font-size:12px; font-weight:600;">' + escapeHtml(u.fullPhone || u.mobileNumber || '—') + '</span>' +
          '</td>' +
          '<td>' +
            '<span class="plan-badge ' + planBadgeClass + '">' + planLabel + '</span>' +
          '</td>' +
          '<td>' +
            statusHtml +
          '</td>' +
          '<td>' +
            '<div style="display:flex; gap:6px;">' +
              (waPhone ? ('<a href="' + waLink + '" target="_blank" class="btn-table btn-wa" style="font-size:11px; padding:4px 8px;">💬 Chat</a>') : '') +
              '<button class="btn-table" data-uid="" + u.userId + "" onclick="openCustomerDrawer(this.dataset.uid)" style="font-size:11px; padding:4px 8px;">⚙️ Manage</button>' +
            '</div>' +
          '</td>' +
        '</tr>';
      }).join("");
    }

    function setQuickFilter(type) {
      selectedQuickFilter = type;
      selectedMonthFilter = null;

      var banner = document.getElementById("activeMonthBanner");
      if (banner) banner.style.display = "none";

      document.querySelectorAll(".pill-btn").forEach(function(b) { b.classList.remove("active"); });
      var pillMap = {
        "ALL": "pillAll",
        "PAID": "pillPaid",
        "MONTHLY": "pillMonthly",
        "YEARLY": "pillYearly",
        "LIFETIME": "pillLifetime",
        "EXPIRING": "pillExpiring",
        "EXPIRED": "pillExpired",
        "TRIAL": "pillTrial"
      };
      var activePill = document.getElementById(pillMap[type]);
      if (activePill) activePill.classList.add("active");

      var planSelect = document.getElementById("planFilter");
      var statusSelect = document.getElementById("statusFilter");

      if (planSelect && statusSelect) {
        if (type === "ALL") {
          planSelect.value = "ALL";
          statusSelect.value = "ALL";
        } else if (type === "PAID") {
          planSelect.value = "PAID";
          statusSelect.value = "ALL";
        } else if (type === "MONTHLY" || type === "YEARLY" || type === "LIFETIME") {
          planSelect.value = type;
          statusSelect.value = "ALL";
        } else if (type === "EXPIRING") {
          planSelect.value = "ALL";
          statusSelect.value = "EXPIRING";
        } else if (type === "EXPIRED") {
          planSelect.value = "ALL";
          statusSelect.value = "EXPIRED";
        } else if (type === "TRIAL") {
          planSelect.value = "TRIAL";
          statusSelect.value = "ALL";
        }
      }

      switchTab('crm');
      applyFilters();
    }

    function syncPillFromSelect() {
      var plan = document.getElementById("planFilter") ? document.getElementById("planFilter").value : "ALL";
      var status = document.getElementById("statusFilter") ? document.getElementById("statusFilter").value : "ALL";

      selectedMonthFilter = null;
      var banner = document.getElementById("activeMonthBanner");
      if (banner) banner.style.display = "none";

      document.querySelectorAll(".pill-btn").forEach(function(b) { b.classList.remove("active"); });
      var pillId = "pillAll";

      if (status === "EXPIRING") {
        pillId = "pillExpiring";
        selectedQuickFilter = "EXPIRING";
      } else if (status === "EXPIRED") {
        pillId = "pillExpired";
        selectedQuickFilter = "EXPIRED";
      } else if (plan === "PAID") {
        pillId = "pillPaid";
        selectedQuickFilter = "PAID";
      } else if (plan === "MONTHLY") {
        pillId = "pillMonthly";
        selectedQuickFilter = "MONTHLY";
      } else if (plan === "YEARLY") {
        pillId = "pillYearly";
        selectedQuickFilter = "YEARLY";
      } else if (plan === "LIFETIME") {
        pillId = "pillLifetime";
        selectedQuickFilter = "LIFETIME";
      } else if (plan === "TRIAL") {
        pillId = "pillTrial";
        selectedQuickFilter = "TRIAL";
      } else {
        selectedQuickFilter = "ALL";
      }

      var el = document.getElementById(pillId);
      if (el) el.classList.add("active");
    }

    function filterByMonth(monthKey) {
      selectedMonthFilter = monthKey;
      selectedQuickFilter = "PAID";

      document.querySelectorAll(".pill-btn").forEach(function(b) { b.classList.remove("active"); });
      var pillPaid = document.getElementById("pillPaid");
      if (pillPaid) pillPaid.classList.add("active");

      var planSelect = document.getElementById("planFilter");
      if (planSelect) planSelect.value = "PAID";

      var statusSelect = document.getElementById("statusFilter");
      if (statusSelect) statusSelect.value = "ALL";

      var banner = document.getElementById("activeMonthBanner");
      if (banner) {
        banner.style.display = "flex";
        banner.innerHTML = '<span>📅 Showing paid orders for <b>' + escapeHtml(monthKey) + '</b></span> <button class="btn btn-secondary" onclick="clearMonthFilter()" style="padding:2px 8px; font-size:11px;">✕ Clear</button>';
      }

      switchTab('crm');
      applyFilters();
      showToast("📅 Filtered customers for month: " + monthKey);
    }

    function clearMonthFilter() {
      selectedMonthFilter = null;
      var banner = document.getElementById("activeMonthBanner");
      if (banner) banner.style.display = "none";
      setQuickFilter('ALL');
    }

    function scrollToMonthlySales() {
      switchTab('analytics');
      var section = document.getElementById("ownerMonthlySalesSection");
      if (section) {
        section.scrollIntoView({ behavior: 'smooth' });
      }
    }

    function updateAnalyticsKPIs() {
      var isOwner = currentUser && currentUser.role === "OWNER";
      var grid = document.getElementById("kpiGrid");

      if (isOwner) {
        var now = Date.now();
        var sevenDays = now + (7 * 24 * 60 * 60 * 1000);

        var gross = 0;
        var mrr = 0;
        var paidCount = 0;
        var monthlyCount = 0;
        var yearlyCount = 0;
        var lifetimeCount = 0;
        var trialCount = 0;
        var activePaidCount = 0;
        var expiredPaidCount = 0;
        var expiringSoonCount = 0;

        rawUsers.forEach(function(u) {
          var plan = normalizeClientPlan(u.planType);
          if (plan !== "TRIAL") {
            var price = u.amountPaid || PLAN_PRICES[plan] || 0;
            gross += price;
            paidCount++;

            if (plan === "MONTHLY") {
              monthlyCount++;
              mrr += 199;
            } else if (plan === "YEARLY") {
              yearlyCount++;
              mrr += Math.round(999 / 12);
            } else if (plan === "LIFETIME") {
              lifetimeCount++;
            }

            if (u.status === "ACTIVE") {
              activePaidCount++;
              if (u.expiryDate > now && u.expiryDate <= sevenDays) {
                expiringSoonCount++;
              }
            } else if (u.status === "EXPIRED" || u.isExpired) {
              expiredPaidCount++;
            }
          } else {
            trialCount++;
          }
        });

        var conversion = rawUsers.length > 0 ? ((paidCount / rawUsers.length) * 100).toFixed(1) : "0";

        grid.innerHTML = '<div class="kpi-card" style="--kpi-accent: #10B981; cursor:pointer;" onclick="scrollToMonthlySales()" title="Click to view Month-Wise Sales Breakdown">' +
            '<div class="kpi-header">' +
              '<span class="kpi-title">Gross Revenue</span>' +
              '<div class="kpi-icon-badge">💰</div>' +
            '</div>' +
            '<div class="kpi-value">₹' + gross.toLocaleString("en-IN") + '</div>' +
            '<div class="kpi-footer">' +
              '<span class="kpi-tag tag-green">Lifetime</span>' +
              '<span>Total collected (Click for breakdown ⬇)</span>' +
            '</div>' +
          '</div>' +

          '<div class="kpi-card" style="--kpi-accent: #3B82F6;">' +
            '<div class="kpi-header">' +
              '<span class="kpi-title">Monthly MRR</span>' +
              '<div class="kpi-icon-badge">🔄</div>' +
            '</div>' +
            '<div class="kpi-value">₹' + mrr.toLocaleString("en-IN") + '</div>' +
            '<div class="kpi-footer">' +
              '<span class="kpi-tag tag-blue">Recurring</span>' +
              '<span>Monthly Run-rate</span>' +
            '</div>' +
          '</div>' +

          '<div class="kpi-card" style="--kpi-accent: #F59E0B; cursor:pointer;" data-filter="PAID" onclick="setQuickFilter(this.dataset.filter)" title="Click to view all Paid Users">' +
            '<div class="kpi-header">' +
              '<span class="kpi-title">Paid Users</span>' +
              '<div class="kpi-icon-badge">👑</div>' +
            '</div>' +
            '<div class="kpi-value">' + paidCount + '</div>' +
            '<div class="kpi-footer">' +
              '<span class="kpi-tag tag-gold">' + conversion + '%</span>' +
              '<span>Click to View All Paid 👉</span>' +
            '</div>' +
          '</div>' +

          '<div class="kpi-card" style="--kpi-accent: #8B5CF6; cursor:pointer;" data-filter="ALL" onclick="setQuickFilter(this.dataset.filter)" title="Click to view all registered users">' +
            '<div class="kpi-header">' +
              '<span class="kpi-title">Total Registered</span>' +
              '<div class="kpi-icon-badge">👥</div>' +
            '</div>' +
            '<div class="kpi-value">' + rawUsers.length + '</div>' +
            '<div class="kpi-footer">' +
              '<span class="kpi-tag tag-purple">Firestore</span>' +
              '<span>Active Profiles</span>' +
            '</div>' +
          '</div>';

        var setEl = function(id, val) { var el = document.getElementById(id); if (el) el.innerText = val; };
        setEl("kpiMonthlyCount", monthlyCount + " Users");
        setEl("kpiMonthlyRevenue", "₹" + (monthlyCount * 199).toLocaleString("en-IN"));
        setEl("kpiYearlyCount", yearlyCount + " Users");
        setEl("kpiYearlyRevenue", "₹" + (yearlyCount * 999).toLocaleString("en-IN"));
        setEl("kpiLifetimeCount", lifetimeCount + " Users");
        setEl("kpiLifetimeRevenue", "₹" + (lifetimeCount * 1999).toLocaleString("en-IN"));
        setEl("kpiTrialCount", trialCount + " Users");

        var mCard = document.getElementById("kpiMonthlyCount") ? document.getElementById("kpiMonthlyCount").closest(".kpi-mini-card") : null;
        if (mCard) { mCard.style.cursor = "pointer"; mCard.onclick = function() { setQuickFilter('MONTHLY'); }; }
        var yCard = document.getElementById("kpiYearlyCount") ? document.getElementById("kpiYearlyCount").closest(".kpi-mini-card") : null;
        if (yCard) { yCard.style.cursor = "pointer"; yCard.onclick = function() { setQuickFilter('YEARLY'); }; }
        var lCard = document.getElementById("kpiLifetimeCount") ? document.getElementById("kpiLifetimeCount").closest(".kpi-mini-card") : null;
        if (lCard) { lCard.style.cursor = "pointer"; lCard.onclick = function() { setQuickFilter('LIFETIME'); }; }
        var tCard = document.getElementById("kpiTrialCount") ? document.getElementById("kpiTrialCount").closest(".kpi-mini-card") : null;
        if (tCard) { tCard.style.cursor = "pointer"; tCard.onclick = function() { setQuickFilter('TRIAL'); }; }

        setEl("pillCountAll", rawUsers.length);
        setEl("pillCountPaid", paidCount);
        setEl("pillCountMonthly", monthlyCount);
        setEl("pillCountYearly", yearlyCount);
        setEl("pillCountLifetime", lifetimeCount);
        setEl("pillCountExpiring", expiringSoonCount);
        setEl("pillCountExpired", expiredPaidCount);
        setEl("pillCountTrial", trialCount);

        renderMonthlySalesSection(currentAnalytics && currentAnalytics.monthlySales ? currentAnalytics.monthlySales : null);
        renderRecentPaidSubscribers();
      } else {
        var phoneLeads = 0;
        var totalContactsSaved = 0;

        rawUsers.forEach(function(u) {
          if (u.fullPhone || u.mobileNumber) phoneLeads++;
          totalContactsSaved += (u.contactsSaved || 0);
        });

        grid.innerHTML = '<div class="kpi-card" style="--kpi-accent: #8B5CF6;">' +
            '<div class="kpi-header">' +
              '<span class="kpi-title">Total Free Leads</span>' +
              '<div class="kpi-icon-badge">👥</div>' +
            '</div>' +
            '<div class="kpi-value">' + rawUsers.length + '</div>' +
            '<div class="kpi-footer">' +
              '<span class="kpi-tag tag-purple">Trial Users</span>' +
              '<span>Ready to Pitch</span>' +
            '</div>' +
          '</div>' +

          '<div class="kpi-card" style="--kpi-accent: #10B981;">' +
            '<div class="kpi-header">' +
              '<span class="kpi-title">Phone Outreach Ready</span>' +
              '<div class="kpi-icon-badge">📱</div>' +
            '</div>' +
            '<div class="kpi-value">' + phoneLeads + '</div>' +
            '<div class="kpi-footer">' +
              '<span class="kpi-tag tag-green">WhatsApp Ready</span>' +
              '<span>With Phone Number</span>' +
            '</div>' +
          '</div>' +

          '<div class="kpi-card" style="--kpi-accent: #3B82F6;">' +
            '<div class="kpi-header">' +
              '<span class="kpi-title">Contacts Saved by Leads</span>' +
              '<div class="kpi-icon-badge">⚡</div>' +
            '</div>' +
            '<div class="kpi-value">' + totalContactsSaved.toLocaleString("en-IN") + '</div>' +
            '<div class="kpi-footer">' +
              '<span class="kpi-tag tag-blue">Usage Activity</span>' +
              '<span>Total Contacts Extracted</span>' +
            '</div>' +
          '</div>' +

          '<div class="kpi-card" style="--kpi-accent: #F59E0B;">' +
            '<div class="kpi-header">' +
              '<span class="kpi-title">Lead Conversion Goal</span>' +
              '<div class="kpi-icon-badge">🎯</div>' +
            '</div>' +
            '<div class="kpi-value">₹199 / mo</div>' +
            '<div class="kpi-footer">' +
              '<span class="kpi-tag tag-gold">Target Plan</span>' +
              '<span>Monthly / Yearly Pitch</span>' +
            '</div>' +
          '</div>';
      }
    }

    function renderCharts() {
      if (typeof Chart === "undefined") {
        console.warn("Chart.js not loaded yet");
        return;
      }
      var isOwner = currentUser && currentUser.role === "OWNER";

      if (isOwner) {
        document.getElementById("donutChartCard").style.display = "";
        var planCounts = { LIFETIME: 0, YEARLY: 0, MONTHLY: 0, TRIAL: 0 };
        rawUsers.forEach(function(u) {
          var p = normalizeClientPlan(u.planType);
          planCounts[p] = (planCounts[p] || 0) + 1;
        });

        // 1. Donut Chart
        var donutCtx = document.getElementById("planDonutChart");
        if (donutCtx) {
          if (donutChartInstance) donutChartInstance.destroy();
          donutChartInstance = new Chart(donutCtx, {
            type: "doughnut",
            data: {
              labels: [
                "Monthly (" + planCounts.MONTHLY + ")",
                "Yearly (" + planCounts.YEARLY + ")",
                "Lifetime (" + planCounts.LIFETIME + ")",
                "Trial (" + planCounts.TRIAL + ")"
              ],
              datasets: [{
                data: [planCounts.MONTHLY, planCounts.YEARLY, planCounts.LIFETIME, planCounts.TRIAL],
                backgroundColor: ["#10B981", "#8B5CF6", "#F59E0B", "#334155"],
                borderWidth: 0
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: {
                legend: { position: "bottom", labels: { color: "#94A3B8", font: { size: 11 } } }
              },
              cutout: "68%"
            }
          });
        }

        // 2. Growth Bar Chart
        var growthCtx = document.getElementById("growthChart");
        if (growthCtx) {
          if (growthChartInstance) growthChartInstance.destroy();
          growthChartInstance = new Chart(growthCtx, {
            type: "bar",
            data: {
              labels: [
                "Trial (" + planCounts.TRIAL + ")",
                "Monthly (" + planCounts.MONTHLY + ")",
                "Yearly (" + planCounts.YEARLY + ")",
                "Lifetime (" + planCounts.LIFETIME + ")"
              ],
              datasets: [{
                label: "Users",
                data: [planCounts.TRIAL, planCounts.MONTHLY, planCounts.YEARLY, planCounts.LIFETIME],
                backgroundColor: ["#334155", "#10B981", "#8B5CF6", "#F59E0B"],
                borderRadius: 6
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: { legend: { display: false } },
              scales: {
                x: { grid: { display: false }, ticks: { color: "#94A3B8", font: { size: 11 } } },
                y: { grid: { color: "#1F293D" }, ticks: { color: "#94A3B8", font: { size: 11 } } }
              }
            }
          });
        }
      } else {
        // MARKETING CHARTS (Lead contact status)
        var withPhone = rawUsers.filter(function(u) { return u.fullPhone || u.mobileNumber; }).length;
        var noPhone = rawUsers.length - withPhone;

        var donutCtx = document.getElementById("planDonutChart");
        if (donutCtx) {
          if (donutChartInstance) donutChartInstance.destroy();
          donutChartInstance = new Chart(donutCtx, {
            type: "doughnut",
            data: {
              labels: ["WhatsApp Ready (" + withPhone + ")", "No Phone (" + noPhone + ")"],
              datasets: [{
                data: [withPhone, noPhone],
                backgroundColor: ["#10B981", "#334155"],
                borderWidth: 0
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: {
                legend: { position: "bottom", labels: { color: "#94A3B8", font: { size: 11 } } }
              },
              cutout: "68%"
            }
          });
        }

        var growthCtx = document.getElementById("growthChart");
        if (growthCtx) {
          if (growthChartInstance) growthChartInstance.destroy();
          growthChartInstance = new Chart(growthCtx, {
            type: "bar",
            data: {
              labels: ["All Leads", "WhatsApp Pitch Ready", "Has Saved Contacts"],
              datasets: [{
                label: "Leads",
                data: [
                  rawUsers.length,
                  withPhone,
                  rawUsers.filter(function(u) { return (u.contactsSaved || 0) > 0; }).length
                ],
                backgroundColor: ["#8B5CF6", "#10B981", "#3B82F6"],
                borderRadius: 6
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: { legend: { display: false } },
              scales: {
                x: { grid: { display: false }, ticks: { color: "#94A3B8", font: { size: 11 } } },
                y: { grid: { color: "#1F293D" }, ticks: { color: "#94A3B8", font: { size: 11 } } }
              }
            }
          });
        }
      }
    }

    function applyFilters() {
      var search = (document.getElementById("tableSearchInput").value || "").toLowerCase().trim();
      var isOwner = currentUser && currentUser.role === "OWNER";
      
      var plan = isOwner ? (document.getElementById("planFilter") ? document.getElementById("planFilter").value : "ALL") : "TRIAL";
      var status = isOwner ? (document.getElementById("statusFilter") ? document.getElementById("statusFilter").value : "ALL") : "ALL";
      var sort = document.getElementById("sortFilter") ? document.getElementById("sortFilter").value : "newest";

      var mFilterEl = document.getElementById("marketingLeadFilter");
      var mFilter = (!isOwner && mFilterEl) ? mFilterEl.value : "ALL";

      var now = Date.now();
      var sevenDays = now + (7 * 24 * 60 * 60 * 1000);

      filteredUsers = rawUsers.filter(function(u) {
        var matchesSearch = !search || 
          (u.name && u.name.toLowerCase().includes(search)) ||
          (u.email && u.email.toLowerCase().includes(search)) ||
          (u.fullPhone && u.fullPhone.includes(search)) ||
          (u.mobileNumber && u.mobileNumber.includes(search)) ||
          (u.userId && u.userId.toLowerCase().includes(search));

        if (!matchesSearch) return false;

        if (isOwner) {
          var uPlan = normalizeClientPlan(u.planType);
          
          // Filter by Month if selected
          if (selectedMonthFilter) {
            if (uPlan === "TRIAL") return false;
            var pTime = u.purchaseDate || u.createdAt;
            var mKey = "2026-09";
            if (pTime > 0) {
              try {
                mKey = new Date(pTime).toISOString().slice(0, 7);
              } catch(e) {}
            }
            if (mKey !== selectedMonthFilter) return false;
          }

          // Plan Filter: Support PAID (all non-trial)
          if (plan === "PAID") {
            if (uPlan === "TRIAL") return false;
          } else if (plan !== "ALL" && uPlan !== plan) {
            return false;
          }

          if (status === "ACTIVE" && u.status !== "ACTIVE") return false;
          if (status === "EXPIRED" && u.status !== "EXPIRED" && !u.isExpired) return false;
          if (status === "TRIAL" && uPlan !== "TRIAL") return false;
          if (status === "EXPIRING") {
            var isExpiring = u.status === "ACTIVE" && u.expiryDate > now && u.expiryDate <= sevenDays;
            if (!isExpiring) return false;
          }
        } else {
          // Marketing Lead Filter options
          if (mFilter === "WHATSAPP") {
            var p = (u.fullPhone || u.mobileNumber || "").replace(/[^0-9]/g, "");
            if (!p) return false;
          } else if (mFilter === "ACTIVE_TRIAL") {
            if ((u.contactsSaved || 0) <= 0) return false;
          } else if (mFilter === "ZERO_CONTACTS") {
            if ((u.contactsSaved || 0) > 0) return false;
          }
        }

        return true;
      });

      // Sorting
      filteredUsers.sort(function(a, b) {
        if (sort === "newest") return (b.createdAt || 0) - (a.createdAt || 0);
        if (sort === "contactsSaved") return (b.contactsSaved || 0) - (a.contactsSaved || 0);
        if (sort === "name") return (a.name || "").localeCompare(b.name || "");
        return 0;
      });

      currentPage = 1;
      renderTable();
    }

    function renderTable() {
      var tbody = document.getElementById("userTableBody");
      var mobileContainer = document.getElementById("mobileCardsContainer");
      var isOwner = currentUser && currentUser.role === "OWNER";
      var total = filteredUsers.length;
      document.getElementById("totalFilteredCount").innerText = total;

      var start = (currentPage - 1) * pageSize;
      var end = Math.min(start + pageSize, total);
      document.getElementById("showingCount").innerText = total === 0 ? "0" : ((start + 1) + "-" + end);
      document.getElementById("pageIndicator").innerText = currentPage + "/" + Math.max(1, Math.ceil(total / pageSize));

      document.getElementById("btnPrevPage").disabled = currentPage <= 1;
      document.getElementById("btnNextPage").disabled = end >= total;

      if (total === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 30px; color:var(--text-muted);">No records found.</td></tr>';
        mobileContainer.innerHTML = '<div style="text-align:center; padding: 30px; color:var(--text-muted); font-size:13px;">No records found.</div>';
        return;
      }

      var paged = filteredUsers.slice(start, end);
      var now = Date.now();

      // Render Desktop Table Rows
      tbody.innerHTML = paged.map(function(u) {
        var initials = (u.name || "U").substring(0, 2).toUpperCase();
        var plan = normalizeClientPlan(u.planType);
        
        var planBadgeClass = "badge-trial";
        var planBadgeText = "⚪ Free Trial";
        if (plan === "LIFETIME") {
          planBadgeClass = "badge-lifetime";
          planBadgeText = "⭐ Lifetime (₹1999)";
        } else if (plan === "YEARLY") {
          planBadgeClass = "badge-yearly";
          planBadgeText = "📆 Yearly (₹999)";
        } else if (plan === "MONTHLY") {
          planBadgeClass = "badge-monthly";
          planBadgeText = "📅 Monthly (₹199)";
        }

        var statusHtml = '<span class="status-badge status-trial">⚪ Free Lead</span>';
        if (isOwner && plan !== "TRIAL") {
          if (u.status === "ACTIVE") {
            if (plan === "LIFETIME") {
              statusHtml = '<span class="status-badge status-active">🟢 Lifetime</span>';
            } else if (u.expiryDate > 0) {
              var daysLeft = Math.ceil((u.expiryDate - now) / (1000 * 60 * 60 * 24));
              if (daysLeft <= 0) {
                statusHtml = '<span class="status-badge status-expired">🔴 Expired</span>';
              } else if (daysLeft <= 7) {
                statusHtml = '<span class="status-badge status-expiring">⚠️ ' + daysLeft + 'd left</span>';
              } else {
                statusHtml = '<span class="status-badge status-active">🟢 Active (' + daysLeft + 'd)</span>';
              }
            } else {
              statusHtml = '<span class="status-badge status-active">🟢 Active</span>';
            }
          } else if (u.status === "EXPIRED" || u.isExpired) {
            statusHtml = '<span class="status-badge status-expired">🔴 Expired</span>';
          } else {
            statusHtml = '<span class="status-badge status-expired">Inactive</span>';
          }
        }

        var rawPhone = (u.fullPhone || u.mobileNumber || "").replace(/[^0-9]/g, "");
        var waPhone = rawPhone;
        if (waPhone.length === 10) waPhone = "91" + waPhone;
        var pitchMsg = encodeURIComponent("Hello " + (u.name || "Friend") + "! Greetings from Auto Contact Saver. Would you like to unlock Unlimited Contact Saving with our special premium discount today?");
        var waLink = waPhone ? ("https://wa.me/" + waPhone + "?text=" + pitchMsg) : "#";

        return '<tr>' +
          '<td>' +
            '<div style="display:flex; align-items:center; gap:10px; cursor:pointer;" data-uid="" + u.userId + "" onclick="openCustomerDrawer(this.dataset.uid)">' +
              '<div class="user-card-avatar" style="width:34px; height:34px; font-size:12px;">' + initials + '</div>' +
              '<div>' +
                '<div style="font-weight:700; color:var(--text-primary); font-size:13.5px;">' + escapeHtml(u.name || 'App User') + '</div>' +
                '<div style="font-size:11px; color:var(--text-muted);">' + escapeHtml(u.email || 'No email') + '</div>' +
              '</div>' +
            '</div>' +
          '</td>' +
          '<td>' +
            '<div style="display:flex; align-items:center; gap:8px;">' +
              '<span style="font-family:monospace; font-size:12px; font-weight:600;">' + escapeHtml(u.fullPhone || u.mobileNumber || '—') + '</span>' +
              (waPhone ? ('<a href="' + waLink + '" target="_blank" class="btn-table btn-wa" style="font-weight:700; padding:4px 8px;" title="WhatsApp Direct Pitch">💬 Pitch</a>') : '') +
            '</div>' +
          '</td>' +
          (isOwner ? ('<td><span class="plan-badge ' + planBadgeClass + '">' + planBadgeText + '</span></td>') : '') +
          '<td>' + statusHtml + '</td>' +
          '<td><b>' + (u.contactsSaved || 0) + '</b> <span style="color:var(--text-muted); font-size:11px;">/ ' + (u.trialContactsLimit || 100) + '</span></td>' +
          '<td>' +
            '<div style="display:flex; gap:6px;">' +
              (waPhone ? ('<a href="' + waLink + '" target="_blank" class="btn-table btn-wa" style="font-size:11.5px; padding:4px 8px;">💬 Chat</a>') : '') +
              '<button class="btn-table" data-uid="" + u.userId + "" onclick="openCustomerDrawer(this.dataset.uid)" style="font-size:11.5px; padding:4px 8px;">' + (isOwner ? '⚙️ Manage' : '👁️ View Lead') + '</button>' +
            '</div>' +
          '</td>' +
        '</tr>';
      }).join("");

      // Render Mobile Card List
      mobileContainer.innerHTML = paged.map(function(u) {
        var initials = (u.name || "U").substring(0, 2).toUpperCase();
        var plan = normalizeClientPlan(u.planType);
        
        var planBadgeClass = "badge-trial";
        var planBadgeText = "⚪ Free Lead";
        if (plan === "LIFETIME") {
          planBadgeClass = "badge-lifetime";
          planBadgeText = "⭐ Lifetime (₹1999)";
        } else if (plan === "YEARLY") {
          planBadgeClass = "badge-yearly";
          planBadgeText = "📆 Yearly (₹999)";
        } else if (plan === "MONTHLY") {
          planBadgeClass = "badge-monthly";
          planBadgeText = "📅 Monthly (₹199)";
        }

        var statusHtml = '<span class="status-badge status-trial">⚪ Free Trial</span>';
        if (isOwner && plan !== "TRIAL") {
          if (u.status === "ACTIVE") {
            if (plan === "LIFETIME") {
              statusHtml = '<span class="status-badge status-active">🟢 Lifetime</span>';
            } else if (u.expiryDate > 0) {
              var daysLeft = Math.ceil((u.expiryDate - now) / (1000 * 60 * 60 * 24));
              if (daysLeft <= 0) {
                statusHtml = '<span class="status-badge status-expired">🔴 Expired</span>';
              } else if (daysLeft <= 7) {
                statusHtml = '<span class="status-badge status-expiring">⚠️ ' + daysLeft + 'd left</span>';
              } else {
                statusHtml = '<span class="status-badge status-active">🟢 Active (' + daysLeft + 'd)</span>';
              }
            } else {
              statusHtml = '<span class="status-badge status-active">🟢 Active</span>';
            }
          } else {
            statusHtml = '<span class="status-badge status-expired">🔴 Expired</span>';
          }
        }

        var rawPhone = (u.fullPhone || u.mobileNumber || "").replace(/[^0-9]/g, "");
        var waPhone = rawPhone;
        if (waPhone.length === 10) waPhone = "91" + waPhone;
        var pitchMsg = encodeURIComponent("Hello " + (u.name || "Friend") + "! Greetings from Auto Contact Saver. Would you like to unlock Unlimited Contact Saving with our special premium discount today?");
        var waLink = waPhone ? ("https://wa.me/" + waPhone + "?text=" + pitchMsg) : "#";

        return '<div class="user-mobile-card" data-uid="" + u.userId + "" onclick="openCustomerDrawer(this.dataset.uid)">' +
          '<div class="user-card-top">' +
            '<div class="user-card-profile">' +
              '<div class="user-card-avatar">' + initials + '</div>' +
              '<div class="user-card-info">' +
                '<div class="name">' + escapeHtml(u.name || 'App User') + '</div>' +
                '<div class="email">' + escapeHtml(u.fullPhone || u.mobileNumber || u.email || 'No contact') + '</div>' +
              '</div>' +
            '</div>' +
            '<div class="user-card-badges">' +
              (isOwner ? ('<span class="plan-badge ' + planBadgeClass + '">' + planBadgeText + '</span>') : '<span class="status-badge status-trial">⚪ Free Lead</span>') +
            '</div>' +
          '</div>' +

          '<div class="user-card-bottom">' +
            '<div style="display:flex; align-items:center; gap:8px;">' +
              statusHtml +
              '<span style="font-size:11.5px; font-weight:600; color:var(--text-secondary);">⚡ Saved: <b>' + (u.contactsSaved || 0) + '</b></span>' +
            '</div>' +
            '<div style="display:flex; gap:6px;" onclick="event.stopPropagation()">' +
              (waPhone ? ('<a href="' + waLink + '" target="_blank" class="btn-table btn-wa" style="padding:5px 10px; font-size:12px; font-weight:700;">💬 Pitch WhatsApp</a>') : '<span style="font-size:11px; color:var(--text-muted); padding:4px;">No phone</span>') +
              '<button class="btn-table" data-uid="" + u.userId + "" onclick="openCustomerDrawer(this.dataset.uid)" style="padding:5px 10px; font-size:12px;">' + (isOwner ? '⚙️ Edit' : '👁️ View') + '</button>' +
            '</div>' +
          '</div>' +
        '</div>';
      }).join("");
    }

    function prevPage() {
      if (currentPage > 1) {
        currentPage--;
        renderTable();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    }

    function nextPage() {
      if (currentPage * pageSize < filteredUsers.length) {
        currentPage++;
        renderTable();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    }

    function filterByPlan(plan) {
      switchTab('crm');
      if (document.getElementById("planFilter")) {
        document.getElementById("planFilter").value = plan;
      }
      if (document.getElementById("statusFilter")) {
        document.getElementById("statusFilter").value = "ALL";
      }
      applyFilters();
    }

    /* Single Customer View Drawer */
    function openCustomerDrawer(userId) {
      selectedUser = rawUsers.find(u => u.userId === userId);
      if (!selectedUser) return;

      const isOwner = currentUser && currentUser.role === "OWNER";

      document.getElementById("drawerAvatar").innerText = (selectedUser.name || "U").substring(0, 2).toUpperCase();
      document.getElementById("drawerName").innerText = selectedUser.name || "App User";
      document.getElementById("drawerEmail").innerText = selectedUser.email || "No Email";
      document.getElementById("drawerPhone").innerText = selectedUser.fullPhone || selectedUser.mobileNumber || "—";
      document.getElementById("drawerUid").innerText = selectedUser.userId;

      const plan = normalizeClientPlan(selectedUser.planType);
      document.getElementById("drawerPlanBadge").innerHTML = isOwner ? 
        \`<span class="plan-badge badge-\${plan.toLowerCase()}">\${plan}</span>\` :
        \`<span class="status-badge status-trial">⚪ Free Trial Lead</span>\`;

      if (isOwner) {
        const expStr = plan === "LIFETIME" ? "Never (Lifetime Plan)" : (selectedUser.expiryDate > 0 ? new Date(selectedUser.expiryDate).toLocaleString("en-IN") : "No Active Expiry");
        document.getElementById("drawerExpiry").innerText = expStr;
        document.getElementById("drawerProvider").innerText = selectedUser.paymentProvider || "NONE";
      }

      document.getElementById("drawerContacts").innerText = \`\${selectedUser.contactsSaved || 0} / \${selectedUser.trialContactsLimit || 100}\`;

      const rawPhone = (selectedUser.fullPhone || selectedUser.mobileNumber || "").replace(/[^0-9]/g, "");
      let waPhone = rawPhone;
      if (waPhone.length === 10) waPhone = "91" + waPhone;
      const waBtn = document.getElementById("drawerWhatsAppBtn");
      if (waPhone) {
        waBtn.style.display = "flex";
        const pitchMsg = encodeURIComponent(\`Hello \${selectedUser.name || 'Friend'}! Greetings from Auto Contact Saver. Would you like to unlock Unlimited Contact Saving with our special premium discount today?\`);
        waBtn.href = \`https://wa.me/\${waPhone}?text=\${pitchMsg}\`;
      } else {
        waBtn.style.display = "none";
      }

      document.getElementById("drawerOverlay").classList.add("open");
    }

    function closeDrawer(e) {
      document.getElementById("drawerOverlay").classList.remove("open");
    }

    async function quickActivatePlan(planId) {
      if (!selectedUser) return;
      if (!confirm(\`Activate \${planId} plan for \${selectedUser.name}?\`)) return;

      const token = getAuthToken();
      try {
        const resp = await fetch("/api/activate-plan", {
          method: "POST",
          headers: { 
            "Content-Type": "application/json",
            "Authorization": \`Bearer \${token}\`
          },
          body: JSON.stringify({ userId: selectedUser.userId, planId })
        });
        const res = await resp.json();
        if (res.success) {
          showToast(\`✅ \${planId} activated successfully!\`);
          closeDrawer();
          loadDashboardData();
        } else {
          showToast("❌ Error: " + res.error);
        }
      } catch (err) {
        showToast("❌ Request failed: " + err.message);
      }
    }

    async function revokeUserPlan() {
      if (!selectedUser) return;
      if (!confirm(\`Revoke plan for \${selectedUser.name} and set to TRIAL?\`)) return;

      const token = getAuthToken();
      try {
        const resp = await fetch("/api/deactivate-plan", {
          method: "POST",
          headers: { 
            "Content-Type": "application/json",
            "Authorization": \`Bearer \${token}\`
          },
          body: JSON.stringify({ userId: selectedUser.userId })
        });
        const res = await resp.json();
        if (res.success) {
          showToast("✅ Plan revoked successfully!");
          closeDrawer();
          loadDashboardData();
        } else {
          showToast("❌ Error: " + res.error);
        }
      } catch (err) {
        showToast("❌ Request failed: " + err.message);
      }
    }

    async function resetUserTrial() {
      if (!selectedUser) return;
      const token = getAuthToken();
      try {
        const resp = await fetch("/api/reset-trial", {
          method: "POST",
          headers: { 
            "Content-Type": "application/json",
            "Authorization": \`Bearer \${token}\`
          },
          body: JSON.stringify({ userId: selectedUser.userId })
        });
        const res = await resp.json();
        if (res.success) {
          showToast("✅ Trial contacts reset to 0/100");
          closeDrawer();
          loadDashboardData();
        } else {
          showToast("❌ Error: " + res.error);
        }
      } catch (err) {
        showToast("❌ Request failed: " + err.message);
      }
    }

    /* Export Hub Functions (Role-Based) */
    function openExportModal() {
      const isOwner = currentUser && currentUser.role === "OWNER";
      const body = document.getElementById("exportModalBody");

      if (isOwner) {
        body.innerHTML = \`
          <p style="font-size:12px; color:var(--text-muted); margin-bottom:4px;">
            Choose which dataset to export in Excel/CSV format (Includes Name, Email, Phone, Expiry, WhatsApp link).
          </p>

          <div class="export-option-card" onclick="exportCategory('EXPIRING')">
            <div class="export-option-info">
              <span class="export-icon">⏳</span>
              <div>
                <div style="font-weight:700; font-size:13.5px;">Expiring Soon Users (&lt;7 Days)</div>
                <div style="font-size:11.5px; color:var(--text-muted);">Users whose subscriptions are ending soon for renewals</div>
              </div>
            </div>
            <span class="kpi-tag tag-gold">Expiring</span>
          </div>

          <div class="export-option-card" onclick="exportCategory('PAID')">
            <div class="export-option-info">
              <span class="export-icon">👑</span>
              <div>
                <div style="font-weight:700; font-size:13.5px;">All Paid Premium Customers</div>
                <div style="font-size:11.5px; color:var(--text-muted);">Active and expired Monthly, Yearly & Lifetime buyers</div>
              </div>
            </div>
            <span class="kpi-tag tag-green">Paid</span>
          </div>

          <div class="export-option-card" onclick="exportCategory('TRIAL')">
            <div class="export-option-info">
              <span class="export-icon">⚪</span>
              <div>
                <div style="font-weight:700; font-size:13.5px;">All Free Trial Leads</div>
                <div style="font-size:11.5px; color:var(--text-muted);">Users currently using the free trial version</div>
              </div>
            </div>
            <span class="kpi-tag tag-purple">Leads</span>
          </div>

          <div class="export-option-card" onclick="exportCategory('FILTERED')">
            <div class="export-option-info">
              <span class="export-icon">🔍</span>
              <div>
                <div style="font-weight:700; font-size:13.5px;">Current Filtered / Search Results</div>
                <div style="font-size:11.5px; color:var(--text-muted);">Whatever is currently shown on your screen table</div>
              </div>
            </div>
            <span class="kpi-tag tag-blue">Filtered</span>
          </div>

          <div class="export-option-card" onclick="exportCategory('ALL')">
            <div class="export-option-info">
              <span class="export-icon">📋</span>
              <div>
                <div style="font-weight:700; font-size:13.5px;">Complete Database (All Users)</div>
                <div style="font-size:11.5px; color:var(--text-muted);">Complete backup of all registered profiles in Firestore</div>
              </div>
            </div>
            <span class="kpi-tag tag-blue">\${rawUsers.length} Users</span>
          </div>
        \`;
      } else {
        // MARKETING ONLY EXPORT
        body.innerHTML = \`
          <p style="font-size:12px; color:var(--text-muted); margin-bottom:4px;">
            Export Free Trial Leads list formatted for WhatsApp & calling marketing campaigns.
          </p>

          <div class="export-option-card" onclick="exportCategory('TRIAL')">
            <div class="export-option-info">
              <span class="export-icon">🎯</span>
              <div>
                <div style="font-weight:700; font-size:13.5px;">All Free Trial Leads (\${rawUsers.length} Leads)</div>
                <div style="font-size:11.5px; color:var(--text-muted);">Includes Name, Phone Number, WhatsApp Link & Contacts Saved</div>
              </div>
            </div>
            <span class="kpi-tag tag-green">Download All</span>
          </div>

          <div class="export-option-card" onclick="exportCategory('FILTERED')">
            <div class="export-option-info">
              <span class="export-icon">🔍</span>
              <div>
                <div style="font-weight:700; font-size:13.5px;">Current Filtered Leads (\${filteredUsers.length} Leads)</div>
                <div style="font-size:11.5px; color:var(--text-muted);">Download whichever leads match your active search or filter</div>
              </div>
            </div>
            <span class="kpi-tag tag-blue">Download Filtered</span>
          </div>
        \`;
      }

      document.getElementById("exportModalOverlay").classList.add("open");
    }

    function closeExportModal(e) {
      document.getElementById("exportModalOverlay").classList.remove("open");
    }

    function exportCategory(type) {
      const now = Date.now();
      const isOwner = currentUser && currentUser.role === "OWNER";
      const sevenDays = now + (7 * 24 * 60 * 60 * 1000);
      let list = [];
      let filename = "AutoContactSaver_Export.csv";

      if (type === "EXPIRING" && isOwner) {
        list = rawUsers.filter(u => normalizeClientPlan(u.planType) !== "TRIAL" && u.status === "ACTIVE" && u.expiryDate > now && u.expiryDate <= sevenDays);
        filename = \`AutoContactSaver_Expiring_Users_\${new Date().toISOString().slice(0, 10)}.csv\`;
      } else if (type === "PAID" && isOwner) {
        list = rawUsers.filter(u => normalizeClientPlan(u.planType) !== "TRIAL");
        filename = \`AutoContactSaver_Paid_Premium_Users_\${new Date().toISOString().slice(0, 10)}.csv\`;
      } else if (type === "TRIAL") {
        list = rawUsers.filter(u => normalizeClientPlan(u.planType) === "TRIAL");
        filename = \`AutoContactSaver_Free_Leads_\${new Date().toISOString().slice(0, 10)}.csv\`;
      } else if (type === "FILTERED") {
        list = filteredUsers.length > 0 ? filteredUsers : rawUsers;
        filename = \`AutoContactSaver_Filtered_Users_\${new Date().toISOString().slice(0, 10)}.csv\`;
      } else {
        list = rawUsers;
        filename = \`AutoContactSaver_\${isOwner ? 'All_Users' : 'All_Leads'}_\${new Date().toISOString().slice(0, 10)}.csv\`;
      }

      if (list.length === 0) {
        showToast("⚠️ No records found in this category to export.");
        return;
      }

      let headers = [];
      let rows = [];

      if (isOwner) {
        headers = [
          "User ID",
          "Full Name",
          "Email Address",
          "Phone Number",
          "Country Code",
          "Plan Type",
          "Status",
          "Purchase Date",
          "Expiry Date",
          "Days Left",
          "Contacts Saved",
          "WhatsApp Link"
        ];

        rows = list.map(u => {
          const plan = normalizeClientPlan(u.planType);
          let daysLeftStr = "—";
          if (plan !== "TRIAL" && u.expiryDate > 0) {
            const dl = Math.ceil((u.expiryDate - now) / (1000 * 60 * 60 * 24));
            daysLeftStr = dl > 0 ? \`\${dl} days\` : "Expired";
          } else if (plan === "LIFETIME") {
            daysLeftStr = "Lifetime";
          }

          const rawPhone = (u.fullPhone || u.mobileNumber || "").replace(/[^0-9]/g, "");
          let waPhone = rawPhone;
          if (waPhone.length === 10) waPhone = "91" + waPhone;
          const waLink = waPhone ? \`https://wa.me/\${waPhone}\` : "";

          return [
            \`"\${u.userId}"\`,
            \`"\${(u.name || 'User').replace(/"/g, '""')}"\`,
            \`"\${u.email || ''}"\`,
            \`"\${u.fullPhone || u.mobileNumber || ''}"\`,
            \`"\${u.countryCode || '+91'}"\`,
            \`"\${plan}"\`,
            \`"\${u.status || 'TRIAL'}"\`,
            \`"\${u.purchaseDate > 0 ? new Date(u.purchaseDate).toLocaleString('en-IN') : ''}"\`,
            \`"\${plan === 'LIFETIME' ? 'LIFETIME' : (u.expiryDate > 0 ? new Date(u.expiryDate).toLocaleString('en-IN') : '')}"\`,
            \`"\${daysLeftStr}"\`,
            \`"\${u.contactsSaved || 0}"\`,
            \`"\${waLink}"\`
          ];
        });
      } else {
        // MARKETING LEAD CSV (Clean & action-oriented)
        headers = [
          "Lead Name",
          "Phone Number",
          "Email Address",
          "Contacts Saved",
          "WhatsApp Direct Pitch Link"
        ];

        rows = list.map(u => {
          const rawPhone = (u.fullPhone || u.mobileNumber || "").replace(/[^0-9]/g, "");
          let waPhone = rawPhone;
          if (waPhone.length === 10) waPhone = "91" + waPhone;
          const pitchMsg = encodeURIComponent(\`Hello \${u.name || 'Friend'}! Greetings from Auto Contact Saver. Would you like to unlock Unlimited Contact Saving with our special premium discount today?\`);
          const waLink = waPhone ? \`https://wa.me/\${waPhone}?text=\${pitchMsg}\` : "";

          return [
            \`"\${(u.name || 'Lead User').replace(/"/g, '""')}"\`,
            \`"\${u.fullPhone || u.mobileNumber || ''}"\`,
            \`"\${u.email || ''}"\`,
            \`"\${u.contactsSaved || 0}"\`,
            \`"\${waLink}"\`
          ];
        });
      }

      const csvContent = "\\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\\r\\n");
      const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.setAttribute("href", url);
      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      closeExportModal();
      showToast(\`📥 Exported \${list.length} records to CSV!\`);
    }

    /* Command Palette (Ctrl + K) */
    function openCommandPalette() {
      document.getElementById("cmdPaletteOverlay").classList.add("open");
      const input = document.getElementById("cmdInput");
      input.value = "";
      input.focus();
      handleCmdSearch();
    }

    function closeCommandPalette(e) {
      document.getElementById("cmdPaletteOverlay").classList.remove("open");
    }

    function handleCmdSearch() {
      const q = (document.getElementById("cmdInput").value || "").toLowerCase().trim();
      const resultsContainer = document.getElementById("cmdResults");

      if (!q) {
        resultsContainer.innerHTML = '<div style="padding:16px; text-align:center; color:var(--text-muted); font-size:12px;">Type to search leads...</div>';
        return;
      }

      const matches = rawUsers.filter(u => 
        (u.name && u.name.toLowerCase().includes(q)) ||
        (u.email && u.email.toLowerCase().includes(q)) ||
        (u.fullPhone && u.fullPhone.includes(q)) ||
        (u.userId && u.userId.toLowerCase().includes(q))
      ).slice(0, 8);

      if (matches.length === 0) {
        resultsContainer.innerHTML = '<div style="padding:16px; text-align:center; color:var(--text-muted); font-size:12px;">No matching records.</div>';
        return;
      }

      resultsContainer.innerHTML = matches.map(u => \`
        <div class="cmd-item" onclick='closeCommandPalette(); openCustomerDrawer("\${u.userId}")'>
          <div>
            <div style="font-weight:600; font-size:13.5px;">\${escapeHtml(u.name || 'User')}</div>
            <div style="font-size:11.5px; color:var(--text-muted);">\${escapeHtml(u.email || u.fullPhone || u.userId)}</div>
          </div>
          <span class="plan-badge badge-\${normalizeClientPlan(u.planType).toLowerCase()}">\${normalizeClientPlan(u.planType)}</span>
        </div>
      \`).join("");
    }

    /* App Remote Config Handler (Owner Only) */
    async function loadAppSettings() {
      try {
        const resp = await fetch("/api/app-settings");
        const data = await resp.json();
        if (data.success) {
          document.getElementById("cfgVersionCode").value = data.latest_version || 12;
          document.getElementById("cfgUpdateAction").value = data.update_action || "SOFT_UPDATE";
        }
      } catch (e) {
        console.error(e);
      }
    }

    async function saveAppSettings() {
      const latest_version = parseInt(document.getElementById("cfgVersionCode").value, 10);
      const update_action = document.getElementById("cfgUpdateAction").value;
      const token = getAuthToken();

      try {
        const resp = await fetch("/api/app-settings", {
          method: "POST",
          headers: { 
            "Content-Type": "application/json",
            "Authorization": \`Bearer \${token}\`
          },
          body: JSON.stringify({ latest_version, update_action })
        });
        const data = await resp.json();
        if (data.success) showToast("✅ App Settings saved!");
        else showToast("❌ Error: " + data.error);
      } catch (err) {
        showToast("❌ Failed to save settings: " + err.message);
      }
    }

    /* Tutorials Handler (Owner Only) */
    let tutorialList = [];
    async function loadTutorials() {
      try {
        const resp = await fetch("/api/tutorials");
        const data = await resp.json();
        tutorialList = data.tutorials || [];
        renderTutorialList();
      } catch (e) {
        console.error(e);
      }
    }

    function renderTutorialList() {
      const container = document.getElementById("tutorialListContainer");
      if (!container) return;

      if (tutorialList.length === 0) {
        container.innerHTML = '<div style="color:var(--text-muted); font-size:12px; padding:12px;">No tutorial videos configured. Click "+ Add Video".</div>';
        return;
      }

      container.innerHTML = tutorialList.map((tut, index) => {
        const vId = extractYouTubeIdClient(tut.videoId || tut.url);
        const thumbUrl = vId ? \`https://img.youtube.com/vi/\${vId}/hqdefault.jpg\` : '';

        return \`
          <div style="background:var(--bg-surface); border:1px solid var(--border-color); border-radius:var(--radius-md); padding:14px; display:flex; gap:14px; align-items:flex-start;">
            \${thumbUrl ? \`
              <div style="width:100px; height:56px; border-radius:8px; overflow:hidden; background:#000; flex-shrink:0; position:relative;">
                <img src="\${thumbUrl}" style="width:100%; height:100%; object-fit:cover;" onerror="this.style.display='none'">
              </div>
            \` : \`
              <div style="width:100px; height:56px; border-radius:8px; background:var(--bg-card); display:flex; align-items:center; justify-content:center; font-size:18px; color:var(--text-muted); flex-shrink:0;">
                📺
              </div>
            \`}

            <div style="flex:1; display:flex; flex-direction:column; gap:8px;">
              <div style="display:flex; justify-content:space-between; align-items:center;">
                <div style="display:flex; align-items:center; gap:6px;">
                  <span style="font-size:11.5px; font-weight:800; color:var(--text-secondary);">VIDEO #\${index + 1}</span>
                  \${vId ? \`<span style="font-size:10.5px; font-family:monospace; background:rgba(59,130,246,0.15); color:#60A5FA; padding:1px 6px; border-radius:4px;">ID: \${vId}</span>\` : ''}
                </div>
                <button class="btn btn-secondary btn-icon" onclick="removeTutorialRow(\${index})" style="color:var(--accent-red); font-size:14px; width:28px; height:28px;">🗑</button>
              </div>

              <input type="text" class="search-input" value="\${escapeHtml(tut.title || '')}" placeholder="Video Title (e.g. How to Save Contacts)" oninput="tutorialList[\${index}].title = this.value">
              
              <div style="display:flex; gap:8px;">
                <input type="text" class="search-input" style="flex:1;" value="\${escapeHtml(tut.url || (tut.videoId ? 'https://youtu.be/' + tut.videoId : ''))}" placeholder="YouTube Link (e.g. https://youtu.be/...)" oninput="updateTutorialUrl(\${index}, this.value)">
                
                <select class="select-dropdown" style="width:110px;" onchange="tutorialList[\${index}].language = this.value">
                  <option value="HINDI" \${(tut.language || '').toUpperCase() === 'HINDI' ? 'selected' : ''}>HINDI</option>
                  <option value="ENGLISH" \${(tut.language || '').toUpperCase() === 'ENGLISH' ? 'selected' : ''}>ENGLISH</option>
                </select>
              </div>
            </div>
          </div>
        \`;
      }).join("");
    }

    function updateTutorialUrl(index, urlVal) {
      tutorialList[index].url = urlVal;
      tutorialList[index].videoId = extractYouTubeIdClient(urlVal);
      clearTimeout(window._tutTimer);
      window._tutTimer = setTimeout(() => {
        renderTutorialList();
      }, 600);
    }

    function addTutorialRow() {
      tutorialList.push({ title: "New Tutorial Video", url: "", videoId: "", language: "HINDI", position: tutorialList.length + 1 });
      renderTutorialList();
    }

    function removeTutorialRow(index) {
      tutorialList.splice(index, 1);
      renderTutorialList();
    }

    async function saveTutorials() {
      const token = getAuthToken();
      const processed = tutorialList.map((t, idx) => ({
        ...t,
        position: idx + 1,
        videoId: extractYouTubeIdClient(t.videoId || t.url),
        language: (t.language || "HINDI").toUpperCase()
      }));

      try {
        const resp = await fetch("/api/tutorials", {
          method: "POST",
          headers: { 
            "Content-Type": "application/json",
            "Authorization": \`Bearer \${token}\`
          },
          body: JSON.stringify({ tutorials: processed })
        });
        const data = await resp.json();
        if (data.success) {
          showToast("✅ Tutorials saved to Firestore live!");
          loadTutorials();
        } else {
          showToast("❌ Error: " + data.error);
        }
      } catch (err) {
        showToast("❌ Failed to save tutorials: " + err.message);
      }
    }

    function showToast(msg) {
      const container = document.getElementById("toastContainer");
      const t = document.createElement("div");
      t.className = "toast";
      t.innerText = msg;
      container.appendChild(t);
      setTimeout(() => t.remove(), 3500);
    }

    function escapeHtml(str) {
      if (!str) return "";
      return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }
  
  </script>
</body>
</html>`;
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    res.writeHead(204, corsHeaders);
    res.end();
    return;
  }

  const reqUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = reqUrl.pathname;

  try {
    // 1. Razorpay Order Creation
    if (req.method === 'POST' && pathname === '/create-order') {
      const body = await parseJson(req);
      const { planId, userId, email } = body;
      let amountPaise = 0;
      let planName = '';

      if (planId === 'MONTHLY') { amountPaise = 19900; planName = 'Monthly Premium Plan'; }
      else if (planId === 'YEARLY') { amountPaise = 99900; planName = 'Yearly Premium Plan'; }
      else if (planId === 'LIFETIME') { amountPaise = 199900; planName = 'Lifetime Premium Plan'; }
      else { return sendJson(res, { success: false, error: 'Invalid plan ID' }, 400); }

      const authHeader = 'Basic ' + Buffer.from(`${RAZORPAY_KEY_ID}:${RAZORPAY_KEY_SECRET}`).toString('base64');
      const rzpResp = await fetch('https://api.razorpay.com/v1/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': authHeader },
        body: JSON.stringify({
          amount: amountPaise,
          currency: 'INR',
          receipt: `rcpt_${Date.now()}`,
          notes: { planId, planName, userId: userId || '', email: email || '', app: 'Auto Contacts Saver' }
        })
      });
      const orderData = await rzpResp.json();
      if (!rzpResp.ok) {
        return sendJson(res, { success: false, error: orderData.error?.description || 'Failed to create order' }, 500);
      }
      return sendJson(res, {
        success: true,
        orderId: orderData.id,
        amount: orderData.amount,
        currency: orderData.currency,
        keyId: RAZORPAY_KEY_ID,
        planId: planId
      });
    }

    // 2. Razorpay Payment Verification
    if (req.method === 'POST' && pathname === '/verify-payment') {
      const body = await parseJson(req);
      const { razorpay_order_id, razorpay_payment_id, razorpay_signature, userId, planId, email } = body;

      if (!razorpay_payment_id || !razorpay_signature) {
        return sendJson(res, { success: false, error: 'Missing verification parameters' }, 400);
      }

      const generatedSignature = crypto
        .createHmac('sha256', RAZORPAY_KEY_SECRET)
        .update(`${razorpay_order_id || ''}|${razorpay_payment_id}`)
        .digest('hex');

      if (generatedSignature !== razorpay_signature) {
        return sendJson(res, { success: false, error: 'Invalid payment signature' }, 400);
      }

      let firestoreResult = { success: false, message: 'No userId provided' };
      if (userId) {
        firestoreResult = await updateAndVerifyFirestorePlanWithRetry(
          userId,
          planId || 'MONTHLY',
          razorpay_payment_id,
          razorpay_order_id || '',
          email || ''
        );
      }

      return sendJson(res, {
        success: true,
        message: 'Payment verified successfully',
        paymentId: razorpay_payment_id,
        orderId: razorpay_order_id,
        firestoreSynced: firestoreResult.success,
        firestoreDetails: firestoreResult
      });
    }

    // 3. Admin Auth Login
    if (req.method === 'POST' && pathname === '/api/auth/login') {
      const body = await parseJson(req);
      const email = (body.email || '').trim().toLowerCase();
      const pin = (body.pin || '').trim();
      const account = AUTH_ACCOUNTS.find(a => a.email.toLowerCase() === email && a.pin === pin);
      if (!account) return sendJson(res, { success: false, error: 'Invalid Email or PIN' }, 401);
      const token = generateAuthToken(account);
      return sendJson(res, {
        success: true,
        token,
        user: { email: account.email, name: account.name, role: account.role }
      });
    }

    // 4. Admin Auth Check
    if (req.method === 'GET' && pathname === '/api/auth/me') {
      const authUser = verifyAuthToken(req.headers['authorization']);
      if (!authUser) return sendJson(res, { success: false, error: 'Unauthenticated' }, 401);
      return sendJson(res, { success: true, user: authUser });
    }

    // 5. Admin Panel HTML
    if (req.method === 'GET' && (pathname === '/' || pathname === '/admin' || pathname === '/admin/')) {
      return sendHtml(res, renderAdminHTML());
    }

    // 6. Users List
    if (req.method === 'GET' && pathname === '/api/users') {
      const authUser = verifyAuthToken(req.headers['authorization']);
      if (!authUser) return sendJson(res, { success: false, error: 'Unauthorized access. Please login.' }, 401);
      const force = reqUrl.searchParams.get('force') === 'true' || reqUrl.searchParams.get('refresh') === 'true';
      const allUsers = await fetchAllUsersFromFirestore(force);
      let filteredUsers = allUsers;
      if (authUser.role === 'MARKETING') {
        filteredUsers = allUsers.filter(u => normalizePlanType(u.planType) === 'TRIAL');
      }
      return sendJson(res, { success: true, role: authUser.role, count: filteredUsers.length, users: filteredUsers });
    }

    // 7. Analytics
    if (req.method === 'GET' && pathname === '/api/analytics') {
      const authUser = verifyAuthToken(req.headers['authorization']);
      if (!authUser) return sendJson(res, { success: false, error: 'Unauthorized' }, 401);
      const force = reqUrl.searchParams.get('force') === 'true';
      const users = await fetchAllUsersFromFirestore(force);
      const analytics = computeAnalytics(users, authUser.role);
      return sendJson(res, { success: true, role: authUser.role, analytics });
    }

    // 8. Plan Activation
    if (req.method === 'POST' && pathname === '/api/activate-plan') {
      const authUser = verifyAuthToken(req.headers['authorization']);
      if (!authUser || authUser.role !== 'OWNER') return sendJson(res, { success: false, error: 'Forbidden' }, 403);
      const body = await parseJson(req);
      const { userId, planId, customExpiryDate } = body;
      if (!userId || !planId) return sendJson(res, { success: false, error: 'Missing parameters' }, 400);
      const result = await activateUserPlanInFirestore(userId, normalizePlanType(planId), customExpiryDate);
      memoryCacheUsers = null;
      return sendJson(res, result, result.success ? 200 : 500);
    }

    // 9. Plan Deactivation
    if (req.method === 'POST' && pathname === '/api/deactivate-plan') {
      const authUser = verifyAuthToken(req.headers['authorization']);
      if (!authUser || authUser.role !== 'OWNER') return sendJson(res, { success: false, error: 'Forbidden' }, 403);
      const body = await parseJson(req);
      const { userId } = body;
      if (!userId) return sendJson(res, { success: false, error: 'Missing userId' }, 400);
      const result = await deactivateUserPlanInFirestore(userId);
      memoryCacheUsers = null;
      return sendJson(res, result, result.success ? 200 : 500);
    }

    // 10. Reset Trial
    if (req.method === 'POST' && pathname === '/api/reset-trial') {
      const authUser = verifyAuthToken(req.headers['authorization']);
      if (!authUser) return sendJson(res, { success: false, error: 'Unauthorized' }, 401);
      const body = await parseJson(req);
      const { userId } = body;
      if (!userId) return sendJson(res, { success: false, error: 'Missing userId' }, 400);
      const result = await resetUserTrialInFirestore(userId);
      memoryCacheUsers = null;
      return sendJson(res, result);
    }

    // 11. User Status Check
    if (req.method === 'GET' && pathname === '/api/user-status') {
      const userId = reqUrl.searchParams.get('userId');
      if (!userId) return sendJson(res, { success: false, error: 'Missing userId parameter' }, 400);
      const statusData = await getUserPlanStatusFromFirestore(userId);
      return sendJson(res, { success: true, ...statusData });
    }

    // 12. App Settings
    if (req.method === 'GET' && pathname === '/api/app-settings') {
      const settings = await getAppSettingsFromFirestore();
      return sendJson(res, { success: true, ...settings });
    }

    if (req.method === 'POST' && pathname === '/api/app-settings') {
      const authUser = verifyAuthToken(req.headers['authorization']);
      if (!authUser || authUser.role !== 'OWNER') return sendJson(res, { success: false, error: 'Forbidden' }, 403);
      const body = await parseJson(req);
      if (body.latest_version === undefined) return sendJson(res, { success: false, error: 'Missing latest_version' }, 400);
      await updateAppSettingsInFirestore(body.latest_version, body.update_action || 'SOFT_UPDATE');
      return sendJson(res, { success: true, message: 'Settings updated' });
    }

    // 13. Tutorials
    if (req.method === 'GET' && pathname === '/api/tutorials') {
      const tutorials = await getTutorialsFromFirestore();
      return sendJson(res, { success: true, tutorials });
    }

    if (req.method === 'POST' && pathname === '/api/tutorials') {
      const authUser = verifyAuthToken(req.headers['authorization']);
      if (!authUser || authUser.role !== 'OWNER') return sendJson(res, { success: false, error: 'Forbidden' }, 403);
      const body = await parseJson(req);
      if (!body.tutorials || !Array.isArray(body.tutorials)) return sendJson(res, { success: false, error: 'Missing tutorials array' }, 400);
      await updateTutorialsInFirestore(body.tutorials);
      return sendJson(res, { success: true, message: 'Tutorials updated' });
    }

    // Default status
    return sendJson(res, {
      status: 'online',
      service: 'Auto Contact Saver Unified VPS Server',
      adminPanel: `http://${req.headers.host || 'localhost'}/admin`
    });

  } catch (err) {
    return sendJson(res, { success: false, error: err.message }, 500);
  }
});

server.listen(PORT, () => {
  console.log(`[Auto Contact Saver] VPS Server listening on port ${PORT}`);
  console.log(`Admin Dashboard: http://localhost:${PORT}/admin`);
});
