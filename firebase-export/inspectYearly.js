const { initializeApp, cert } = require("firebase-admin/app");
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

async function inspectYearlyUsers() {
  const usersSnapshot = await db.collection("users").get();
  console.log("=== INSPECTING ALL USERS FOR YEARLY/ANNUAL/PLANS ===");
  usersSnapshot.forEach(doc => {
    const data = doc.data();
    const plan = (data.planType || "").toUpperCase();
    if (plan.includes("YEAR") || plan.includes("ANNUAL") || plan.includes("365") || plan.includes("999") || data.email === "suuqhaye@gmail.com") {
      console.log(`Doc ID: ${doc.id}`);
      console.log("Data:", JSON.stringify(data, null, 2));
    }
  });
  process.exit(0);
}

inspectYearlyUsers().catch(err => {
  console.error(err);
  process.exit(1);
});
