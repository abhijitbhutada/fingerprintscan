var exec = require('cordova/exec');

module.exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'fingerprintscan', 'coolMethod', [arg0]);
};

module.exports.scan = function (arg0, success, error) {
    exec(success, error, 'fingerprintscan', 'scan', [arg0]);

}