const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const fs = require("fs");
const path = require("path");

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

async function main() {
  console.log("Connecting to Firestore...");

  // 1. Check all collections
  const collections = await db.listCollections();
  console.log("Found collections:", collections.map(c => c.id));

  // 2. Fetch all collections data
  const fullData = {};

  for (const col of collections) {
    const snap = await col.get();
    console.log(`Collection '${col.id}' has ${snap.size} documents.`);
    const docs = [];
    snap.forEach(doc => {
      docs.push({ _docId: doc.id, ...doc.data() });
    });
    fullData[col.id] = docs;
  }

  // 3. Save full raw export to JSON
  const rawExportPath = path.join(__dirname, "firebase_full_export_september_2026.json");
  fs.writeFileSync(rawExportPath, JSON.stringify(fullData, null, 2));
  console.log(`Saved complete Firestore export to: ${rawExportPath}`);

  // 4. Specifically analyze 'users' and any payment/orders collection
  const users = fullData["users"] || [];
  console.log(`Total users in Firestore: ${users.length}`);

  const PLAN_PRICING = {
    MONTHLY: 199,
    YEARLY: 999,
    LIFETIME: 1999
  };

  const allPaidUsers = [];
  const sept2026Sales = [];

  // Helper to parse date
  function parseToDate(val) {
    if (!val) return null;
    if (typeof val === 'number') {
      // Could be seconds or milliseconds
      return val < 10000000000 ? new Date(val * 1000) : new Date(val);
    }
    if (val._seconds) {
      return new Date(val._seconds * 1000);
    }
    if (typeof val === 'string') {
      const d = new Date(val);
      if (!isNaN(d.getTime())) return d;
    }
    return null;
  }

  users.forEach(u => {
    const planType = (u.planType || u.plan || "TRIAL").toUpperCase();
    const isPaid = planType === "MONTHLY" || planType === "YEARLY" || planType === "LIFETIME";

    if (isPaid) {
      // Determine purchase date:
      // Priority 1: u.purchaseDate or u.paymentDate or u.paidAt
      // Priority 2: from expiryDate (for MONTHLY: expiry - 30 days, for YEARLY: expiry - 365 days)
      // Priority 3: u.createdAt or u.updatedAt
      let purchaseDate = parseToDate(u.purchaseDate || u.paymentDate || u.paidAt);
      const expiryDate = parseToDate(u.expiryDate);

      let inferred = false;
      if (!purchaseDate && expiryDate) {
        if (planType === "MONTHLY") {
          purchaseDate = new Date(expiryDate.getTime() - (30 * 24 * 60 * 60 * 1000));
          inferred = true;
        } else if (planType === "YEARLY") {
          purchaseDate = new Date(expiryDate.getTime() - (365 * 24 * 60 * 60 * 1000));
          inferred = true;
        } else if (planType === "LIFETIME") {
          purchaseDate = parseToDate(u.createdAt) || new Date("2026-08-01T00:00:00Z");
          inferred = true;
        }
      }

      if (!purchaseDate && u.createdAt) {
        purchaseDate = parseToDate(u.createdAt);
        inferred = true;
      }

      const amount = Number(u.amountPaid || u.amount || u.price || PLAN_PRICING[planType] || 0);

      const record = {
        userId: u._docId || u.userId || u.uid,
        name: u.name || u.displayName || "Unknown User",
        email: u.email || "N/A",
        phone: u.mobileNumber || u.phone || u.fullPhone || u.phoneNumber || "N/A",
        planType: planType,
        status: u.status || (u.isActive ? "ACTIVE" : "INACTIVE"),
        amountPaid: amount,
        purchaseDate: purchaseDate ? purchaseDate.toISOString() : null,
        purchaseDateLocalIST: purchaseDate ? purchaseDate.toLocaleString("en-IN", { timeZone: "Asia/Kolkata" }) : null,
        expiryDate: expiryDate ? expiryDate.toISOString() : null,
        expiryDateLocalIST: expiryDate ? expiryDate.toLocaleString("en-IN", { timeZone: "Asia/Kolkata" }) : null,
        inferredPurchaseDate: inferred,
        paymentId: u.paymentId || u.razorpayPaymentId || u.razorpay_payment_id || null,
        rawFields: {
          purchaseDate: u.purchaseDate,
          paymentDate: u.paymentDate,
          createdAt: u.createdAt,
          expiryDate: u.expiryDate,
          amountPaid: u.amountPaid
        }
      };

      allPaidUsers.push(record);

      // Check if purchaseDate is in September 2026 (IST or UTC: 2026-09)
      if (purchaseDate) {
        const year = purchaseDate.getFullYear();
        const month = purchaseDate.getMonth() + 1; // 1-12
        if (year === 2026 && month === 9) {
          sept2026Sales.push(record);
        }
      }
    }
  });

  // Sort September sales by purchase date ascending
  sept2026Sales.sort((a, b) => new Date(a.purchaseDate).getTime() - new Date(b.purchaseDate).getTime());

  // Aggregate stats
  const septStats = {
    month: "September 2026",
    totalSalesCount: sept2026Sales.length,
    totalRevenue: sept2026Sales.reduce((acc, s) => acc + s.amountPaid, 0),
    plansBreakdown: {
      MONTHLY: {
        count: sept2026Sales.filter(s => s.planType === "MONTHLY").length,
        revenue: sept2026Sales.filter(s => s.planType === "MONTHLY").reduce((acc, s) => acc + s.amountPaid, 0)
      },
      YEARLY: {
        count: sept2026Sales.filter(s => s.planType === "YEARLY").length,
        revenue: sept2026Sales.filter(s => s.planType === "YEARLY").reduce((acc, s) => acc + s.amountPaid, 0)
      },
      LIFETIME: {
        count: sept2026Sales.filter(s => s.planType === "LIFETIME").length,
        revenue: sept2026Sales.filter(s => s.planType === "LIFETIME").reduce((acc, s) => acc + s.amountPaid, 0)
      }
    },
    sales: sept2026Sales
  };

  // Month-wise breakdown across all history for comparison
  const monthWiseSummary = {};
  allPaidUsers.forEach(p => {
    const d = p.purchaseDate ? new Date(p.purchaseDate) : null;
    const mKey = d ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}` : "Unknown";
    if (!monthWiseSummary[mKey]) {
      monthWiseSummary[mKey] = { count: 0, revenue: 0, monthly: 0, yearly: 0, lifetime: 0 };
    }
    monthWiseSummary[mKey].count++;
    monthWiseSummary[mKey].revenue += p.amountPaid;
    if (p.planType === "MONTHLY") monthWiseSummary[mKey].monthly++;
    if (p.planType === "YEARLY") monthWiseSummary[mKey].yearly++;
    if (p.planType === "LIFETIME") monthWiseSummary[mKey].lifetime++;
  });

  const fullReport = {
    generatedAt: new Date().toISOString(),
    totalPaidUsersInFirestore: allPaidUsers.length,
    september2026: septStats,
    monthWiseSummary: monthWiseSummary
  };

  const reportPath = path.join(__dirname, "september_2026_sales_report.json");
  fs.writeFileSync(reportPath, JSON.stringify(fullReport, null, 2));
  console.log(`Saved September 2026 Sales Report to: ${reportPath}`);

  console.log("\n==================== SEPTEMBER 2026 SALES REPORT ====================");
  console.log(`Total Sales in September 2026: ${septStats.totalSalesCount}`);
  console.log(`Total Revenue: ₹${septStats.totalRevenue}`);
  console.log(`  - Monthly Plans (₹199): ${septStats.plansBreakdown.MONTHLY.count} (₹${septStats.plansBreakdown.MONTHLY.revenue})`);
  console.log(`  - Yearly Plans (₹999):  ${septStats.plansBreakdown.YEARLY.count} (₹${septStats.plansBreakdown.YEARLY.revenue})`);
  console.log(`  - Lifetime Plans (₹1999): ${septStats.plansBreakdown.LIFETIME.count} (₹${septStats.plansBreakdown.LIFETIME.revenue})`);
  console.log("======================================================================\n");
  console.log("Month-wise summary across database:");
  console.table(monthWiseSummary);
}

main().catch(err => {
  console.error("Execution error:", err);
  process.exit(1);
});
