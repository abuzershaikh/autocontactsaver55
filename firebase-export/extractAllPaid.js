const fs = require('fs');

const content = fs.readFileSync('C:\\Users\\Abuzar\\.gemini\\antigravity\\brain\\da218d80-77f1-49cf-9793-dab4e2102618\\.system_generated\\logs\\transcript_full.jsonl', 'utf8');
const lines = content.split('\n');

const allFoundPaid = [];

// Look for step 3464 and step 4128
for (const line of lines) {
  if (line.includes('"step_index":3464')) {
    const obj = JSON.parse(line);
    // Parse step 3464 table if available
    console.log("Found step 3464, length:", obj.content?.length);
  }
}

// Let's also check step 4128
for (const line of lines) {
  if (line.includes('"step_index":4128')) {
    const obj = JSON.parse(line);
    const raw = obj.content;
    const blocks = raw.split('{\n');
    blocks.forEach((b, idx) => {
      if (idx === 0) return;
      const blk = '{\n' + b.split('}\n')[0] + '}';
      const nameMatch = blk.match(/name:\s*'([^']*)'/);
      const phoneMatch = blk.match(/phone:\s*'([^']*)'/);
      const planMatch = blk.match(/plan:\s*'([^']*)'/);
      const statusMatch = blk.match(/status:\s*'([^']*)'/);
      const expiryMatch = blk.match(/expiry:\s*([0-9]+)/);
      const isActMatch = blk.match(/isActive:\s*(true|false)/);

      if (planMatch) {
        allFoundPaid.push({
          userId: "usr_" + Math.random().toString(36).substring(2, 10),
          name: nameMatch ? nameMatch[1] : "App User",
          email: "",
          mobileNumber: phoneMatch ? phoneMatch[1] : "",
          fullPhone: phoneMatch ? phoneMatch[1] : "",
          planType: planMatch[1],
          status: statusMatch ? statusMatch[1] : "ACTIVE",
          isActive: isActMatch ? isActMatch[1] === "true" : true,
          expiryDate: expiryMatch ? parseInt(expiryMatch[1], 10) : 0
        });
      }
    });
  }
}

console.log("Total paid extracted from 4128:", allFoundPaid.length);
fs.writeFileSync('d:\\antigravity project\\Autocontactssaver2\\firebase-export\\extractedPaid.json', JSON.stringify(allFoundPaid, null, 2));
