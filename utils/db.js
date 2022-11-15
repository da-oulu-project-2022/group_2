import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth, signInWithPopup, GoogleAuthProvider } from "firebase/auth";
import * as config from "../config/default.js"
const app = initializeApp(config.default.firebaseConfig)
const db = getFirestore(app);
const auth = getAuth(app);
//const analytics = getAnalytics(app);
export default db;

export { auth }