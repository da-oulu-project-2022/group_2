import express from 'express';

import * as userController from '../controllers/userController.js';
const userRouters = express.Router();

// router.post('/student', addStudent);
userRouters.get('/', userController.getAllUsers);
// router.get('/student/:id', getStudent);
userRouters.put('/:id', userController.userUpdate);
// router.delete('/student/:id', deleteStudent);
// router.get("/abc", (req, res, next) => {
//     console.log("qwerty");
//     res.status(200).send("hello");
//     next()
// });

export { userRouters }