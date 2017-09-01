fs = require('fs');
try {
	obj = JSON.parse(fs.readFileSync('/dev/stdin').toString())
	if (obj.value != "") {
		obj.value = obj.value + 1
	}
} catch(e) {}
console.log(JSON.stringify(obj));
