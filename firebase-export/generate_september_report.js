const fs = require('fs');
const path = require('path');

const users = JSON.parse(fs.readFileSync(path.join(__dirname, 'all_users_firebase_export.json'), 'utf8'));
const authCsv = fs.readFileSync('C:\\Users\\Abuzar\\Downloads\\firebase_users_export.csv', 'utf8');
const authLines = authCsv.split('\n').slice(1).filter(l => l.trim().length > 0);

const authList = [];
for (const line of authLines) {
  // simple csv split handling quotes
  const matches = line.match(/(".*?"|[^",\s]+)(?=\s*,|\s*$)/g) || line.split(',');
  const clean = matches.map(m => m.replace(/^"|"$/g, '').trim());
  if (clean.length >= 2) {
    authList.push({
      uid: clean[0] || '',
      email: clean[1] || '',
      displayName: clean[2] || '',
      phoneNumber: clean[3] || '',
      creationTime: clean[4] || '',
      provider: clean[6] || ''
    });
  }
}

const septSales = [];

users.forEach(u => {
  const p = (u.planType || '').toUpperCase();
  if (p === 'MONTHLY' || p === 'YEARLY' || p === 'LIFETIME') {
    let pTime = u.purchaseDate || u.createdAt;
    if (!pTime && u.expiryDate && p === 'MONTHLY') pTime = u.expiryDate - (30 * 24 * 60 * 60 * 1000);
    if (!pTime && u.expiryDate && p === 'YEARLY') pTime = u.expiryDate - (365 * 24 * 60 * 60 * 1000);

    const d = pTime ? new Date(pTime) : null;
    if (d && !isNaN(d.getTime())) {
      const monthKey = d.toISOString().slice(0, 7);
      if (monthKey === '2026-09') {
        let matchedAuth = authList.find(a => 
          (a.displayName && u.name && a.displayName.toLowerCase().trim() === u.name.toLowerCase().trim()) ||
          (a.email && u.email && a.email.toLowerCase() === u.email.toLowerCase()) ||
          (a.uid && (a.uid === u.userId))
        );

        if (!matchedAuth && u.name) {
          matchedAuth = authList.find(a => a.displayName && a.displayName.toLowerCase().includes(u.name.toLowerCase()));
        }

        septSales.push({
          orderNumber: 0,
          userId: u.userId,
          name: u.name || (matchedAuth ? matchedAuth.displayName : 'App User'),
          email: (matchedAuth && matchedAuth.email) || u.email || 'N/A',
          mobileNumber: (matchedAuth && matchedAuth.phoneNumber) || u.mobileNumber || u.fullPhone || 'N/A',
          planType: u.planType,
          amountPaidINR: u.amountPaid || (p === 'MONTHLY' ? 199 : (p === 'YEARLY' ? 999 : 1999)),
          status: u.status || 'ACTIVE',
          contactsSaved: u.contactsSaved || 0,
          purchaseDateISO: d.toISOString(),
          purchaseDateIST: d.toLocaleString('en-IN', { timeZone: 'Asia/Kolkata', dateStyle: 'medium', timeStyle: 'short' }),
          purchaseDateOnly: d.toISOString().split('T')[0],
          expiryDateISO: u.expiryDate ? new Date(u.expiryDate).toISOString() : 'Lifetime',
          expiryDateIST: u.expiryDate ? new Date(u.expiryDate).toLocaleString('en-IN', { timeZone: 'Asia/Kolkata', dateStyle: 'medium' }) : 'Lifetime',
          paymentProvider: u.paymentProvider || 'RAZORPAY',
          authProvider: matchedAuth ? matchedAuth.provider : 'google.com'
        });
      }
    }
  }
});

// Sort chronologically
septSales.sort((a, b) => new Date(a.purchaseDateISO).getTime() - new Date(b.purchaseDateISO).getTime());
septSales.forEach((s, idx) => s.orderNumber = idx + 1);

const totalRevenue = septSales.reduce((sum, s) => sum + s.amountPaidINR, 0);

const fullReport = {
  reportMetadata: {
    title: 'Auto Contact Saver - September 2026 Sales & Revenue Analysis',
    period: 'September 1, 2026 - September 26, 2026 (Month-to-Date)',
    generatedAt: new Date().toISOString(),
    dataSource: 'Firebase Firestore + Firebase Auth Production Dataset',
    currency: 'INR (₹)'
  },
  executiveSummary: {
    totalOrders: septSales.length,
    totalRevenueINR: totalRevenue,
    averageRevenuePerUser: Math.round(totalRevenue / septSales.length),
    planBreakdown: {
      MONTHLY: {
        totalOrders: septSales.filter(s => s.planType === 'MONTHLY').length,
        pricePerPlanINR: 199,
        subtotalINR: septSales.filter(s => s.planType === 'MONTHLY').length * 199
      },
      YEARLY: {
        totalOrders: septSales.filter(s => s.planType === 'YEARLY').length,
        pricePerPlanINR: 999,
        subtotalINR: septSales.filter(s => s.planType === 'YEARLY').length * 999
      },
      LIFETIME: {
        totalOrders: septSales.filter(s => s.planType === 'LIFETIME').length,
        pricePerPlanINR: 1999,
        subtotalINR: septSales.filter(s => s.planType === 'LIFETIME').length * 1999
      }
    }
  },
  dailySalesBreakdown: {},
  customerOrders: septSales
};

septSales.forEach(s => {
  const day = s.purchaseDateOnly;
  if (!fullReport.dailySalesBreakdown[day]) {
    fullReport.dailySalesBreakdown[day] = { ordersCount: 0, dailyRevenueINR: 0, customers: [] };
  }
  fullReport.dailySalesBreakdown[day].ordersCount++;
  fullReport.dailySalesBreakdown[day].dailyRevenueINR += s.amountPaidINR;
  fullReport.dailySalesBreakdown[day].customers.push({
    orderNumber: s.orderNumber,
    name: s.name,
    email: s.email,
    phone: s.mobileNumber,
    amount: s.amountPaidINR
  });
});

const outPath = path.join(__dirname, 'september_2026_complete_sales_report.json');
fs.writeFileSync(outPath, JSON.stringify(fullReport, null, 2));

console.log('Saved to ' + outPath);
console.log('Total Orders:', septSales.length);
console.log('Total Revenue: ₹' + totalRevenue);
