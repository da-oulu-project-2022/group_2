import express from 'express';

import * as auth from "../middlewares/login.js"
const authRoutes = express.Router();

// router.post('/student', addStudent);

authRoutes.post('/login', auth.basedLogin);
authRoutes.post('/logout', auth.logout);
authRoutes.post('/register', auth.basedRegister);
// router.delete('/student/:id', deleteStudent);
// router.get("/abc", (req, res, next) => {
//     console.log("qwerty");
//     res.status(200).send("hello");
//     next()
// });

export { authRoutes }