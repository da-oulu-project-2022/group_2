'use strict';

import db from '../utils/db.js';
import User from "../models/users.js"
import * as apiResponse from "../helpers/apiReponse.js"
import { collection, getDocs, doc, updateDoc } from "firebase/firestore";

const collectionName = "users"
    // const addStudent = async(req, res, next) => {
    //     try {
    //         const data = req.body;
    //         await firestore.collection('userList').doc().set(data);
    //         res.send('Record saved successfuly');
    //     } catch (error) {
    //         res.status(400).send(error.message);
    //     }
    // }

const getAllUsers = async(req, res, next) => {
    try {
        const userList = await getDocs(collection(db, collectionName));
        const menuArray = [];
        if (userList.empty) {
            res.status(404).send('No user record found');
        } else {
            userList.forEach(doc => {
                const menu = new User(
                    doc.id,
                    doc.data().username,

                );
                menuArray.push(menu);
            });

            return apiResponse.successResponseWithData(res, "Operation success", menuArray);


        }
    } catch (error) {
        return apiResponse.ErrorResponse(res, error.message);
    }
    next()
}

// const getStudent = async(req, res, next) => {
//     try {
//         const id = req.params.id;
//         const student = await firestore.collection('userList').doc(id);
//         const data = await student.get();
//         if (!data.exists) {
//             res.status(404).send('Student with the given ID not found');
//         } else {
//             res.send(data.data());
//         }
//     } catch (error) {
//         res.status(400).send(error.message);
//     }
// }

const userUpdate = async(req, res, next) => {

    try {
        const id = req.params.id;

        const data = req.body;

        const docRef = doc(db, collectionName, id)

        await updateDoc(docRef, data)

        return apiResponse.successResponse(res, 'User record updated successfuly');
    } catch (error) {
        return apiResponse.ErrorResponse(res, error.message);
    }
}

// const deleteStudent = async(req, res, next) => {
//     try {
//         const id = req.params.id;
//         await firestore.collection('userList').doc(id).delete();
//         res.send('Record deleted successfuly');
//     } catch (error) {
//         res.status(400).send(error.message);
//     }
// }
export { getAllUsers, userUpdate }