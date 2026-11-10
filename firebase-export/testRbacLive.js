async function testAuth() {
  const baseUrl = "https://autocontactsaver-admin-worker.zestbizar.workers.dev";

  console.log("=== 1. Testing Owner Login ===");
  const ownerRes = await fetch(`${baseUrl}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "owner@autocontactsaver.com", pin: "123456" })
  });
  const ownerData = await ownerRes.json();
  console.log("Owner Login Result:", ownerData.success, "Role:", ownerData.user?.role);

  if (ownerData.token) {
    const usersRes = await fetch(`${baseUrl}/api/users`, {
      headers: { "Authorization": `Bearer ${ownerData.token}` }
    });
    const usersData = await usersRes.json();
    console.log("Owner Users Count (All Users):", usersData.count, "Sample user plan:", usersData.users?.[0]?.planType);

    const analyticsRes = await fetch(`${baseUrl}/api/analytics`, {
      headers: { "Authorization": `Bearer ${ownerData.token}` }
    });
    const analyticsData = await analyticsRes.json();
    console.log("Owner Analytics Gross Revenue:", analyticsData.analytics?.grossRevenue, "MRR:", analyticsData.analytics?.mrr);
  }

  console.log("\n=== 2. Testing Marketing Login ===");
  const mktRes = await fetch(`${baseUrl}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "marketing@autocontactsaver.com", pin: "654321" })
  });
  const mktData = await mktRes.json();
  console.log("Marketing Login Result:", mktData.success, "Role:", mktData.user?.role);

  if (mktData.token) {
    const mktUsersRes = await fetch(`${baseUrl}/api/users`, {
      headers: { "Authorization": `Bearer ${mktData.token}` }
    });
    const mktUsersData = await mktUsersRes.json();
    console.log("Marketing Users Count (Trial Only):", mktUsersData.count);
    
    // Check if any paid user exists in marketing list
    const hasPaid = (mktUsersData.users || []).some(u => u.planType !== "TRIAL");
    console.log("Has any paid user in Marketing response? ->", hasPaid ? "⚠️ LEAKED" : "✅ 100% PROTECTED (Zero Paid Users)");

    const mktAnalyticsRes = await fetch(`${baseUrl}/api/analytics`, {
      headers: { "Authorization": `Bearer ${mktData.token}` }
    });
    const mktAnalyticsData = await mktAnalyticsRes.json();
    console.log("Marketing Analytics:", JSON.stringify(mktAnalyticsData.analytics));
  }
}

testAuth().catch(console.error);
