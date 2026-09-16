const admin = require("firebase-admin");
admin.initializeApp({ credential: admin.credential.cert(require("../service-account.json")) });
admin.auth().updateUser("oSBlO03R6OVDCJxhCt9NV2VFKWn1", { password: "DemoObjetivo123!" })
  .then(() => { console.log("password seteado"); process.exit(0); });
