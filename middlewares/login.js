import { signInWithPopup, GoogleAuthProvider, getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword, onAuthStateChanged, signOut } from "firebase/auth";
import * as apiResponse from "../helpers/apiReponse.js"
const auth = getAuth();
const provider = new GoogleAuthProvider();

const basedRegister = async(req, res, next) => {
    const email = req.body.email
    const password = req.body.password
    createUserWithEmailAndPassword(auth, email, password)
        .then((userCredential) => {
            // Signed in 
            const user = userCredential.user;
            return apiResponse.successResponseWithData(res, "Signed in", user.accessToken)
                // ...
        })
        .catch((error) => {
            const errorCode = error.code;
            const errorMessage = error.message;
            // ..
            return apiResponse.unauthorizedResponse(res, errorCode)
        });
}

const basedLogin = async(req, res, next) => {
    const email = req.body.email
    const password = req.body.password
    signInWithEmailAndPassword(auth, email, password)
        .then((userCredential) => {
            // Signed in 
            const user = userCredential.user;
            // return res.status(200).send(user.accessToken);
            return apiResponse.successResponseWithData(res, "Signed in", user.accessToken)
                // ...
        })
        .catch((error) => {
            const errorCode = error.code;
            const errorMessage = error.message;
            // ..
            // return res.status(400).send(errorCode);
            return apiResponse.unauthorizedResponse(res, errorCode)
        });
}

const checkAuth = (req, res, next) => {
    if (auth.currentUser) {
        // User is signed in, see docs for a list of available properties
        // https://firebase.google.com/docs/reference/js/firebase.User
        const data = { uid: auth.currentUser.uid, email: auth.currentUser.email };
        console.log(data)
        next()
    } else {
        return apiResponse.unauthorizedResponse(res, "Unauthenticated")

    }

}



const logout = (req, res, next) => {
    signOut(auth).then(() => {
        // res.status(200).send("Logout successfully !");
        return apiResponse.successResponse(res, "Logout successfully !")
    }).catch((error) => {
        return apiResponse.notFoundResponse(res, "Logout failed!")
    });

}

export { basedLogin, checkAuth, logout, basedRegister }