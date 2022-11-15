import express from 'express';

import * as CONFIG from "./config/default.js"
import { userRouters } from "./routes/user.js"
import { authRoutes } from "./routes/authentication.js"
import { checkAuth } from "./middlewares/login.js"
import path from 'path';
import { fileURLToPath } from 'url';
import bodyParser from 'body-parser'

const app = express()

const __filename = fileURLToPath(
    import.meta.url);

const __dirname = path.dirname(__filename);

app.use(express.static(path.join(__dirname, "public")));


app.use(express.urlencoded({ extended: true }));
app.use(express.json())


app.use("/auth", authRoutes)

app.use('/user', userRouters)

app.get('/', function(req, res) {

    res.send('Hello World')

})


app.get('/checkauth', checkAuth, function(req, res) {

    res.send('Hello, u signed in')

})



app.listen(CONFIG.default.port, () => {
    console.log(`Server is running at port ${CONFIG.default.port}: ${CONFIG.default.url}`)
})