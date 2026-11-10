const { initializeApp, cert } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore } = require("firebase-admin/firestore");

const serviceAccount = {
  type: "service_account",
  project_id: "auto-contact-saver-b8a12",
  private_key_id: "4118274f7c7ecca19eb86d5430599592024b20f1",
  private_key: "-----BEGIN PRIVATE KEY-----\nMIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDcZfGbS92YFvne\nQnE8G5dlBs00it0EjqVn5XVvKIxynn2mBJKB5b5QzxNA3rlYsjytOqaL8tlmMHRz\nhUykA4FGt6HODF+Dt3u06vsQIOj9uDoAUZzJmlBvUJqRAaZ7GjjWULxhVR51DBYA\nsclKIu8VhajrrLrvgXBIgpFLVEgtTh7RAjHpZCoEvAeWDDctmp+GaQ4ib7ZVd09J\nhhC5uPx3CSfeh6WPJ1IbySbCstj8O714DnHv1JixaF0tMHTInGbJnZ8bVPXuRKX1\nvlVG5nyKuIHB2yIWc9uY2ls8EVis/NYCjGZN9ZUbmo1B0oojQRBty+oV0vOrRNgJ\nnthhjCmdAgMBAAECggEAEz4rXB3BmTfYVRmD/XWDQGTZoc7GAVHbCzkziaMGsDM6\nUTZw/Dps5W1PBwjrMd1OAunuBKYNaSit4jAvQtqzfNMno2/dHwj0GhWyGfRlo1Fk\nbwaxI8D/aNimxiZYvMLLV5rw96aQ5DNL8H+VTY3DRiPHXdxU7fhlL0+HFAalzp6r\n/y6kmzn4xB4aCDyc7c3geL5j57dFsEoYDFbhJnYHo2Q+h7dy6U2yMnsdwMC3nHaI\nnBgeK3KK6f7KmyFQqr5LUrSdMJAj7bOe3nZ83NbHNqrHxIDEsvBb1vQb3Vxvllal\n5s5egQ7LxU/AefU35doY0O4TWAnKZRL2dP4g1HdagQKBgQD0hyktLZdJhNl5NMOn\n2t4eOhHa2xZa/4X81CHF02tq51gkJlBXORtJxYLb1ppIMu+cIv7R3CPcAeM6urUY\nxlT3fnh1/BT+wXB6CJh9P1dc/MU06nlCAbW5wEbOPpEMbOWpMXeq/Qr/HCL3qA2f\nLuPV8ZYpL7tEG3/cPMmbhG77sQKBgQDmvPqVKKuuNU9bLsnOHnHHEHDDKBFxMDEx\n9U42y9qVWGYI0lhLtSLfQhU/YqKGtjtZf0M+NMQMYzUc/hKthZdtGe81mLP/gPWc\nfMptIcrknM3g3zuGQcr7fX0k8xuuSPA9qcmZm6mtKxCacg3ZjDXqzYG1h6QmFTYy\nHbIremkDrQKBgQCTW5he12iiL2L2/esijmzofkogP4MbV30hPZ0UoDHNfJjLQCOO\nTKW7MUGIxay38aofx58UTLt3a9PJopZb1bmCZD6nqz/jviLQB7oNUEyPhuw41Uw5\nkNo4lDyXMXErsbw818irzE4CKqyP3kBHO3Sliy7YpyENDB1jVWaRR5+yIQKBgQCh\nlmbGXp0ceyj/6lxZhOiZR+IwSbctVVqeafREW67QN//83ZIIMuog/95xbVn0jVoy\n6vTkc394TWG81BGwg4XwWeEz36TuohlfZeU9WBUG1Ok5OhwtgZXtWYxye+4+t8XG\n6UjN0j2ZkDcocgdQ+ugnb5QO1NcX9HLn1fQYjrlMYQKBgQCoG0JAV6lAy/dVdid8\nmpzP272NXWIFVH4+1PJfr/Wz22QsbiqwH8I7OnLBOoLfLySL368bKhNHfedbzl2W\n3osUzxu7u7BCuETfboYfqL634j9qtY0oK7yeNGA/EEss77/5O0/CXUqtorXBpPR2\n7VvN1qPXb3dLLrdBsySQpVfQZg==\n-----END PRIVATE KEY-----\n",
  client_email: "firebase-adminsdk-fbsvc@auto-contact-saver-b8a12.iam.gserviceaccount.com",
  client_id: "106184732523133790059",
  auth_uri: "https://accounts.google.com/o/oauth2/auth",
  token_uri: "https://oauth2.googleapis.com/token",
};

initializeApp({
  credential: cert(serviceAccount)
});

const db = getFirestore();
const auth = getAuth();

async function checkAnalytics() {
  console.log("=== CHECKING FIRESTORE & AUTH STATS ===");

  // 1. List all collections
  const collections = await db.listCollections();
  console.log("Collections found in Firestore:", collections.map(c => c.id));

  // 2. Fetch all users from Auth
  const authUsers = [];
  let pageToken;
  do {
    const list = await auth.listUsers(1000, pageToken);
    authUsers.push(...list.users);
    pageToken = list.pageToken;
  } while (pageToken);
  console.log(`Total Firebase Auth Registered Users: ${authUsers.length}`);

  // 3. Fetch all documents from 'users' collection
  const usersSnapshot = await db.collection("users").get();
  console.log(`Total Firestore 'users' Documents: ${usersSnapshot.size}`);

  const userStats = {
    totalUsers: usersSnapshot.size,
    trial: 0,
    monthly: 0,
    yearly: 0,
    lifetime: 0,
    otherPlans: {},
    activePremiumUsers: [],
    expiredPremiumUsers: [],
    allPremiumRecords: []
  };

  const now = Date.now();

  usersSnapshot.forEach(doc => {
    const data = doc.data();
    const planType = (data.planType || "TRIAL").toUpperCase();
    const isActive = data.isActive === true || data.status === "ACTIVE";
    const expiryDate = data.expiryDate || 0;
    const isExpired = expiryDate > 0 && expiryDate < now;

    if (planType === "TRIAL") {
      userStats.trial++;
    } else if (planType === "MONTHLY") {
      userStats.monthly++;
    } else if (planType === "YEARLY") {
      userStats.yearly++;
    } else if (planType === "LIFETIME") {
      userStats.lifetime++;
    } else {
      userStats.otherPlans[planType] = (userStats.otherPlans[planType] || 0) + 1;
    }

    if (planType !== "TRIAL") {
      const record = {
        id: doc.id,
        email: data.email || "",
        name: data.name || "",
        phone: data.mobileNumber || data.phone || data.fullPhone || "",
        planType: planType,
        isActive: isActive,
        isExpired: isExpired,
        purchaseDate: data.purchaseDate ? new Date(data.purchaseDate).toISOString() : (data.paymentDate ? new Date(data.paymentDate).toISOString() : "N/A"),
        expiryDate: expiryDate ? new Date(expiryDate).toISOString() : "N/A",
        paymentId: data.paymentId || data.razorpayPaymentId || "N/A",
        orderId: data.orderId || data.razorpayOrderId || "N/A",
        amountPaid: data.amountPaid || data.amount || data.price || "N/A"
      };
      userStats.allPremiumRecords.push(record);
      if (isActive && !isExpired) {
        userStats.activePremiumUsers.push(record);
      } else {
        userStats.expiredPremiumUsers.push(record);
      }
    }
  });

  // 4. Check if there are other collections like 'orders', 'payments', 'transactions', 'subscriptions'
  for (const col of collections) {
    if (col.id !== "users") {
      const snap = await col.get();
      console.log(`\nCollection '${col.id}': ${snap.size} documents`);
      if (snap.size > 0 && snap.size <= 20) {
        snap.forEach(d => {
          console.log(` - Doc [${d.id}]:`, JSON.stringify(d.data()));
        });
      }
    }
  }

  console.log("\n================ SUMMARY ================");
  console.log(`Total Auth Users: ${authUsers.length}`);
  console.log(`Total Firestore User Profiles: ${userStats.totalUsers}`);
  console.log(`Trial Users: ${userStats.trial}`);
  console.log(`Monthly Plan Users: ${userStats.monthly}`);
  console.log(`Yearly Plan Users: ${userStats.yearly}`);
  console.log(`Lifetime Plan Users: ${userStats.lifetime}`);
  console.log(`Other Plans:`, JSON.stringify(userStats.otherPlans));
  console.log(`Total Premium Plan Buyers (Lifetime/Monthly/Yearly): ${userStats.allPremiumRecords.length}`);
  console.log(`Active Premium Users Right Now: ${userStats.activePremiumUsers.length}`);
  console.log(`Expired / Inactive Premium Users: ${userStats.expiredPremiumUsers.length}`);
  
  console.log("\n=== DETAILED PREMIUM USERS LIST ===");
  console.log(JSON.stringify(userStats.allPremiumRecords, null, 2));

  process.exit(0);
}

checkAnalytics().catch(err => {
  console.error("Error running analytics:", err);
  process.exit(1);
});
