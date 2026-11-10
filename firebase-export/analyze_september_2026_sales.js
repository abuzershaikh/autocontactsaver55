const fs = require('fs');
const path = require('path');

// Load full users dataset from live worker or fallbackUsers
const workerUrl = "https://autocontactsaver-admin-worker.zestbizar.workers.dev";

async function run() {
  console.log("Fetching full dataset from API with Owner Token...");
  
  // Login as owner
  const loginResp = await fetch(`${workerUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'owner@autocontactsaver.com', pin: '80978097' })
  });
  const loginData = await loginResp.json();
  if (!loginData.success || !loginData.token) {
    throw new Error("Failed to login: " + JSON.stringify(loginData));
  }

  // Fetch /api/users
  const usersResp = await fetch(`${workerUrl}/api/users`, {
    headers: { Authorization: `Bearer ${loginData.token}` }
  });
  const usersData = await usersResp.json();
  const allUsers = usersData.users || [];
  console.log(`Successfully retrieved ${allUsers.length} total users.`);

  // Save the complete downloaded JSON
  const allUsersJsonPath = path.join(__dirname, "all_users_firebase_export.json");
  fs.writeFileSync(allUsersJsonPath, JSON.stringify(allUsers, null, 2));
  console.log(`Saved complete JSON to: ${allUsersJsonPath}`);

  // Fetch /api/analytics
  const analyticsResp = await fetch(`${workerUrl}/api/analytics`, {
    headers: { Authorization: `Bearer ${loginData.token}` }
  });
  const analyticsData = await analyticsResp.json();
  const analytics = analyticsData.analytics || {};

  // Identify September 2026 Sales
  // September 2026 range: 2026-09-01T00:00:00 to 2026-09-30T23:59:59
  const septSales = [];
  const augSales = [];
  const julSales = [];
  const otherSales = [];

  const paidUsers = allUsers.filter(u => {
    const p = (u.planType || "").toUpperCase();
    return p === "MONTHLY" || p === "YEARLY" || p === "LIFETIME";
  });

  console.log(`Total Paid Users across database: ${paidUsers.length}`);

  paidUsers.forEach(u => {
    // Determine purchase timestamp
    let pTime = u.purchaseDate;
    if (!pTime && u.createdAt) pTime = u.createdAt;
    if (!pTime && u.expiryDate && u.planType === "MONTHLY") pTime = u.expiryDate - (30 * 24 * 60 * 60 * 1000);
    if (!pTime && u.expiryDate && u.planType === "YEARLY") pTime = u.expiryDate - (365 * 24 * 60 * 60 * 1000);

    const d = pTime ? new Date(pTime) : null;
    let monthKey = "Unknown";
    let formattedDate = "N/A";
    let formattedDateIST = "N/A";

    if (d && !isNaN(d.getTime())) {
      monthKey = d.toISOString().slice(0, 7); // e.g. 2026-09
      formattedDate = d.toISOString().split('T')[0];
      formattedDateIST = d.toLocaleString("en-IN", { timeZone: "Asia/Kolkata", dateStyle: "medium", timeStyle: "short" });
    }

    const saleRecord = {
      userId: u.userId,
      name: u.name || "Customer",
      mobileNumber: u.mobileNumber || u.fullPhone || "N/A",
      email: u.email || "N/A",
      planType: u.planType,
      amountPaid: u.amountPaid || (u.planType === "MONTHLY" ? 199 : (u.planType === "YEARLY" ? 999 : 1999)),
      status: u.status,
      purchaseDateTimestamp: pTime,
      purchaseDate: formattedDate,
      purchaseDateIST: formattedDateIST,
      expiryDate: u.expiryDate ? new Date(u.expiryDate).toISOString().split('T')[0] : "Lifetime",
      paymentProvider: u.paymentProvider || "Razorpay"
    };

    if (monthKey === "2026-09") {
      septSales.push(saleRecord);
    } else if (monthKey === "2026-08") {
      augSales.push(saleRecord);
    } else if (monthKey === "2026-07") {
      julSales.push(saleRecord);
    } else {
      otherSales.push(saleRecord);
    }
  });

  // Sort September sales chronologically
  septSales.sort((a, b) => (a.purchaseDateTimestamp || 0) - (b.purchaseDateTimestamp || 0));

  // Daily breakdown for September 2026
  const septDailyBreakdown = {};
  septSales.forEach(s => {
    const day = s.purchaseDate || "Unknown";
    if (!septDailyBreakdown[day]) {
      septDailyBreakdown[day] = { count: 0, revenue: 0, customers: [] };
    }
    septDailyBreakdown[day].count++;
    septDailyBreakdown[day].revenue += s.amountPaid;
    septDailyBreakdown[day].customers.push({
      name: s.name,
      phone: s.mobileNumber,
      plan: s.planType,
      amount: s.amountPaid
    });
  });

  const septRevenue = septSales.reduce((acc, s) => acc + s.amountPaid, 0);
  const septMonthlyCount = septSales.filter(s => s.planType === "MONTHLY").length;
  const septYearlyCount = septSales.filter(s => s.planType === "YEARLY").length;
  const septLifetimeCount = septSales.filter(s => s.planType === "LIFETIME").length;

  const finalAnalysis = {
    reportTitle: "September 2026 Sales & Revenue Analysis",
    generatedAt: new Date().toISOString(),
    databaseSource: "Firebase Firestore / Auto Contact Saver Production DB",
    summary: {
      totalSalesCount: septSales.length,
      totalRevenueINR: septRevenue,
      averageOrderValue: septSales.length > 0 ? Math.round(septRevenue / septSales.length) : 0,
      plansBreakdown: {
        MONTHLY: {
          count: septMonthlyCount,
          pricePerUnit: 199,
          totalRevenue: septMonthlyCount * 199
        },
        YEARLY: {
          count: septYearlyCount,
          pricePerUnit: 999,
          totalRevenue: septYearlyCount * 999
        },
        LIFETIME: {
          count: septLifetimeCount,
          pricePerUnit: 1999,
          totalRevenue: septLifetimeCount * 1999
        }
      }
    },
    monthOverMonthComparison: {
      "July 2026": {
        salesCount: julSales.length,
        totalRevenue: julSales.reduce((acc, s) => acc + s.amountPaid, 0)
      },
      "August 2026": {
        salesCount: augSales.length,
        totalRevenue: augSales.reduce((acc, s) => acc + s.amountPaid, 0)
      },
      "September 2026 (Month-to-Date)": {
        salesCount: septSales.length,
        totalRevenue: septRevenue
      }
    },
    septemberDailyBreakdown: septDailyBreakdown,
    allSeptemberCustomers: septSales
  };

  const analysisJsonPath = path.join(__dirname, "september_2026_sales_analysis.json");
  fs.writeFileSync(analysisJsonPath, JSON.stringify(finalAnalysis, null, 2));
  console.log(`Saved detailed September 2026 Sales Analysis JSON to: ${analysisJsonPath}`);

  console.log("\n==================== SEPTEMBER 2026 SALES REPORT ====================");
  console.log(`Total Sales in September 2026: ${finalAnalysis.summary.totalSalesCount} Orders`);
  console.log(`Total Revenue in September 2026: ₹${finalAnalysis.summary.totalRevenueINR}`);
  console.log(`Monthly Plans: ${septMonthlyCount} × ₹199 = ₹${septMonthlyCount * 199}`);
  console.log(`Yearly Plans: ${septYearlyCount} × ₹999 = ₹${septYearlyCount * 999}`);
  console.log(`Lifetime Plans: ${septLifetimeCount} × ₹1999 = ₹${septLifetimeCount * 1999}`);
  console.log("======================================================================\n");
  console.log("Daily breakdown in September 2026:");
  console.table(Object.entries(septDailyBreakdown).map(([day, val]) => ({
    Date: day,
    SalesCount: val.count,
    RevenueINR: `₹${val.revenue}`,
    CustomerNames: val.customers.map(c => c.name).join(', ')
  })));

  console.log("\nAll September Customers List:");
  console.table(septSales.map((s, idx) => ({
    "#": idx + 1,
    Name: s.name,
    Phone: s.mobileNumber,
    Plan: s.planType,
    Amount: `₹${s.amountPaid}`,
    Date: s.purchaseDate,
    Status: s.status
  })));
}

run().catch(console.error);
