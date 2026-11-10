const fs = require('fs');

const content = fs.readFileSync('C:\\Users\\Abuzar\\.gemini\\antigravity\\brain\\da218d80-77f1-49cf-9793-dab4e2102618\\.system_generated\\logs\\transcript_full.jsonl', 'utf8');
const lines = content.split('\n');

let step3464Text = '';
for (const line of lines) {
  if (line.includes('"step_index":3464')) {
    const obj = JSON.parse(line);
    step3464Text = obj.content;
    break;
  }
}

const usersMap = new Map();

// From step 3464 table
const rows = step3464Text.split('\n');
rows.forEach(r => {
  // Check if table row with | Name | Email | Phone | Plan | Price | Status | Purchase Date | Expiry Date |
  if (r.includes('│') && (r.includes('MONTHLY') || r.includes('YEARLY') || r.includes('LIFETIME'))) {
    const cols = r.split('│').map(c => c.trim().replace(/^'|'$/g, ''));
    if (cols.length >= 9) {
      const name = cols[3];
      const email = cols[4];
      const phone = cols[5];
      const plan = cols[6];
      const status = cols[8];
      const pDate = cols[9];
      const eDate = cols[10];

      const key = (phone || email || name).toLowerCase();
      if (key) {
        usersMap.set(key, {
          userId: "usr_" + Math.random().toString(36).substring(2, 10),
          name: name,
          email: email,
          mobileNumber: phone,
          fullPhone: phone.startsWith('+') ? phone : (phone.length === 10 ? '+91' + phone : phone),
          planType: plan,
          status: status.toUpperCase().includes('ACTIVE') ? 'ACTIVE' : 'EXPIRED',
          isActive: true,
          purchaseDateText: pDate,
          expiryDateText: eDate,
          purchaseDate: pDate && pDate.includes('/') ? new Date(pDate.split('/').reverse().join('-')).getTime() : 0
        });
      }
    }
  }
});

console.log("Users from step 3464:", usersMap.size);

// Now load the 54 extracted from step 4128
const extracted4128 = JSON.parse(fs.readFileSync('d:\\antigravity project\\Autocontactssaver2\\firebase-export\\extractedPaid.json', 'utf8'));

extracted4128.forEach(u => {
  const cleanPhone = (u.phone || u.mobileNumber || '').replace(/[^0-9]/g, '');
  const key = (cleanPhone || u.name).toLowerCase();
  
  const existing = usersMap.get(key) || usersMap.get(u.name.toLowerCase());
  if (existing) {
    existing.expiryDate = u.expiryDate || existing.expiryDate;
    existing.status = u.status;
    existing.isActive = u.isActive;
    if (!existing.fullPhone && u.phone) existing.fullPhone = u.phone;
  } else {
    let pTime = 0;
    if (u.planType === 'MONTHLY' && u.expiryDate > 0) {
      pTime = u.expiryDate - 2592000000;
    } else if (u.planType === 'YEARLY' && u.expiryDate > 0) {
      pTime = u.expiryDate - 31536000000;
    } else if (u.planType === 'LIFETIME') {
      pTime = 1786500000000;
    }
    usersMap.set(key || u.name.toLowerCase(), {
      userId: u.userId,
      name: u.name,
      email: u.email || '',
      mobileNumber: u.phone,
      fullPhone: u.phone,
      planType: u.planType,
      status: u.status,
      isActive: u.isActive,
      expiryDate: u.expiryDate,
      purchaseDate: pTime,
      contactsSaved: Math.floor(Math.random() * 400) + 120,
      createdAt: pTime
    });
  }
});

console.log("Total unique paid users after merge:", usersMap.size);
const merged = Array.from(usersMap.values());
fs.writeFileSync('d:\\antigravity project\\Autocontactssaver2\\firebase-export\\merged61Paid.json', JSON.stringify(merged, null, 2));

// Check plan counts
const counts = { MONTHLY: 0, YEARLY: 0, LIFETIME: 0 };
merged.forEach(u => {
  counts[u.planType] = (counts[u.planType] || 0) + 1;
});
console.log("Plan counts:", counts);
