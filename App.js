var http = require('http'); // Import Node.js core module
const url = require('url');

var data = undefined;
const server = http.createServer((req, res) => {
    let url = req.url.split('?');
    console.log(url);
    if (url.includes('/set')) {
        data = `${url[1]}:${url[2]}`;
        res.writeHead(200, { 'Content-Type': 'text/plain' });
        res.write(`${url[1]}:${url[2]}`);
        res.end();
    } else if(url.includes('/get')) {
        res.writeHead(200, { 'Content-Type': 'text/plain' });
        res.write(`${data}`);
        res.end();
    }else {
        res.writeHead(200, { 'Content-Type': 'text/plain' });
        res.write('page is ready to write');
        res.end();
    }
 });
 
server.listen(3000);
 
console.log('http://localhost:3000/')
console.log('to set: /set?<from X, Y>?<to X, Y>')