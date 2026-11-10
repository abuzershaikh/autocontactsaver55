const fs = require('fs');

const content = fs.readFileSync('C:\\Users\\Abuzar\\.gemini\\antigravity\\brain\\da218d80-77f1-49cf-9793-dab4e2102618\\.system_generated\\logs\\transcript_full.jsonl', 'utf8');
const lines = content.split('\n');
let rawOutput = '';
for (const line of lines) {
  if (line.includes('"step_index":4128')) {
    const obj = JSON.parse(line);
    rawOutput = obj.content;
    break;
  }
}

// Extract objects from rawOutput
const userBlocks = rawOutput.split('{\n').slice(1);
const parsedUsers = [];

userBlocks.forEach(b => {
  const block = '{\n' + b.split('}\n')[0] + '}';
  try {
    const nameMatch = block.match(/name:\s*'([^']*)'/);
    const phoneMatch = block.match(/phone:\s*'([^']*)'/);
    const planMatch = block.match(/plan:\s*'([^']*)'/);
    const statusMatch = block.match(/status:\s*'([^']*)'/);
    const expiryMatch = block.match(/expiry:\s*([0-9]+)/);
    
    if (planMatch) {
      const plan = planMatch[1];
      const expiry = expiryMatch ? parseInt(expiryMatch[1], 10) : 0;
      let purchaseTime = 0;
      if (plan === 'MONTHLY' && expiry > 0) {
        purchaseTime = expiry - (30 * 24 * 60 * 60 * 1000);
      } else if (plan === 'YEARLY' && expiry > 0) {
        purchaseTime = expiry - (365 * 24 * 60 * 60 * 1000);
      } else if (plan === 'LIFETIME') {
        purchaseTime = 1786500000000; // August 2026 approx
      }

      parsedUsers.push({
        name: nameMatch ? nameMatch[1] : 'App User',
        phone: phoneMatch ? phoneMatch[1] : '',
        plan: plan,
        status: statusMatch ? statusMatch[1] : 'ACTIVE',
        expiry: expiry,
        purchaseTime: purchaseTime,
        month: purchaseTime > 0 ? new Date(purchaseTime).toISOString().slice(0, 7) : 'Unknown'
      });
    }
  } catch (e) {}
});

console.log("Parsed users count:", parsedUsers.length);
const months = {};
const planPricing = { MONTHLY: 199, YEARLY: 999, LIFETIME: 1999 };

parsedUsers.forEach(u => {
  if (!months[u.month]) {
    months[u.month] = { revenue: 0, count: 0, plans: { MONTHLY: 0, YEARLY: 0, LIFETIME: 0 } };
  }
  months[u.month].count++;
  months[u.month].revenue += planPricing[u.plan] || 0;
  months[u.month].plans[u.plan] = (months[u.month].plans[u.plan] || 0) + 1;
});

console.log("Month-wise Sales Breakdown:", JSON.stringify(months, null, 2));
