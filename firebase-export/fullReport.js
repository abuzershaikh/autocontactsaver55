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

async function fullReport() {
  const usersSnapshot = await db.collection("users").get();
  const users = [];

  usersSnapshot.forEach(doc => {
    users.push({ id: doc.id, ...doc.data() });
  });

  const PLAN_PRICES = {
    "MONTHLY": 199,
    "YEARLY": 999,
    "LIFETIME": 1999
  };

  const premiumUsers = users.filter(u => u.planType && u.planType.toUpperCase() !== "TRIAL");
  const trialUsers = users.filter(u => !u.planType || u.planType.toUpperCase() === "TRIAL");

  const monthlyList = premiumUsers.filter(u => u.planType.toUpperCase() === "MONTHLY");
  const yearlyList = premiumUsers.filter(u => u.planType.toUpperCase() === "YEARLY");
  const lifetimeList = premiumUsers.filter(u => u.planType.toUpperCase() === "LIFETIME");

  const now = Date.now();

  let totalEstimatedRevenue = 0;
  monthlyList.forEach(() => totalEstimatedRevenue += 199);
  yearlyList.forEach(() => totalEstimatedRevenue += 999);
  lifetimeList.forEach(() => totalEstimatedRevenue += 1999);

  console.log("=== FIRESTORE DETAILED METRICS REPORT ===");
  console.log(`Total Registered Users in Firestore: ${users.length}`);
  console.log(`Total Trial Users: ${trialUsers.length}`);
  console.log(`Total Premium Users (Purchased / Activated): ${premiumUsers.length}`);
  console.log(`  ├─ Monthly Plan (₹199): ${monthlyList.length} users (Total: ₹${monthlyList.length * 199})`);
  console.log(`  ├─ Yearly Plan (₹999): ${yearlyList.length} users (Total: ₹${yearlyList.length * 999})`);
  console.log(`  └─ Lifetime Plan (₹1999): ${lifetimeList.length} users (Total: ₹${lifetimeList.length * 1999})`);
  console.log(`Total Gross Sales / Estimated Revenue: ₹${totalEstimatedRevenue}`);

  console.log("\n=== ALL PREMIUM USERS TABLE ===");
  const tableData = premiumUsers.map((u, i) => {
    const isAct = u.isActive === true || u.status === "ACTIVE";
    const exp = u.expiryDate || 0;
    const isExp = exp > 0 && exp < now;
    const statusStr = !isAct ? "Inactive" : (isExp ? "Expired" : "Active");
    const pDate = u.purchaseDate ? new Date(u.purchaseDate).toLocaleDateString('en-IN') : (u.paymentDate ? new Date(u.paymentDate).toLocaleDateString('en-IN') : "N/A");
    const eDate = exp > 0 ? new Date(exp).toLocaleDateString('en-IN') : "Never (Lifetime)";
    const price = PLAN_PRICES[u.planType.toUpperCase()] || 0;

    return {
      "#": i + 1,
      Name: u.name || "N/A",
      Email: u.email || "N/A",
      Phone: u.mobileNumber || u.phone || u.fullPhone || "N/A",
      Plan: u.planType,
      Price: `₹${price}`,
      Status: statusStr,
      "Purchase Date": pDate,
      "Expiry Date": eDate
    };
  });

  console.table(tableData);

  // Check Razorpay/payment fields if any
  console.log("\n=== PAYMENT / ORDER METADATA CHECK ===");
  const withPaymentIds = premiumUsers.filter(u => u.paymentId || u.razorpayPaymentId || u.orderId);
  console.log(`Users with explicit Payment/Order IDs stored: ${withPaymentIds.length}`);
  withPaymentIds.forEach(u => {
    console.log(`- ${u.email} (${u.name}): Plan=${u.planType}, PaymentID=${u.paymentId || u.razorpayPaymentId}, OrderID=${u.orderId || u.razorpayOrderId}`);
  });

  process.exit(0);
}

fullReport().catch(err => {
  console.error(err);
  process.exit(1);
});
