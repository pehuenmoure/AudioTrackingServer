// var express = require('express');       // web server application
// var app = express();
// var http = require('http');
// var fs = require('fs');
var child_process = require('child_process');
var request = require("request");

// const https = require('https');
// const options = {
//   key: fs.readFileSync('test-key.pem'),
//   cert: fs.readFileSync('test-cert.pem'),
// };

// var httpsServer = https.createServer(options, app);
// httpsServer.listen(443);
// // var io = require('socket.io')(httpsServer); 
// var ioclient = require('socket.io-client')('https://andrea-pi.local', { secure: true });
// ioclient.on('connect', function(){
// 	console.log("io connected");
// });

var java = child_process.spawn('java', ['Main']);
var lastAngle = 0;
var audioOut = [ 0, 0, 0, 0, 0, 0 ];
var audioThreshold = 0.8;
var audioIndex = 0;
java.stdout.on('data', function(data){
	// console.log('stdout: ' + data);
	audioOut[audioIndex] = parseFloat(data);
	audioIndex = (audioIndex + 1) % audioOut.length;
	var avg = 0;
	for(var i in audioOut){
		avg += audioOut[i];
	}
	avg /= audioOut.length;

	if(avg < -1* audioThreshold){
		// set robot left
		console.log('set robot left');
		if (lastAngle != 45){
			// ioclient.emit('setPanAngle', '045');
			request.post("http://localhost/setPanAngle?a=045");
			lastAngle = 45;
		}
	} else if(avg > audioThreshold){
		// set robot right
		console.log('set robot right');
		if (lastAngle != 135){
			// ioclient.emit('setPanAngle', '135');
			request.post("http://localhost/setPanAngle?a=135");
			lastAngle = 135;
		}
	}
});
function exitHandler(message){
	java.kill('SIGINT');
}

//do something when app is closing
process.on('exit', exitHandler.bind(null,{cleanup:true}));

//catches ctrl+c event
process.on('SIGINT', exitHandler.bind(null, {exit:true}));

//catches uncaught exceptions
process.on('uncaughtException', exitHandler.bind(null, {exit:true}));

// http.createServer(function (req, res) {
//     res.writeHead(301, { "Location": "https://" + req.headers['host'] + req.url });
//     res.end();
// }).listen(80);

// console.log("server started");