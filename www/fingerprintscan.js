var exec = require('cordova/exec');

module.exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'fingerprintscan', 'coolMethod', [arg0]);
};

module.exports.scanfinger = function (arg0, success, error) {
    exec(success, error, 'fingerprintscan', 'scanfinger', [arg0]);

}
module.exports.initialise = function (arg0, success, error) {
    exec(success, error, 'fingerprintscan', 'initialise', [arg0]);

}
module.exports.matchFingers = function (arg0, success, error) {
    exec(success, error, 'fingerprintscan', 'matchFingers', [arg0]);

}
module.exports.registerDevice = function (arg0, success, error) {
    exec(success, error, 'fingerprintscan', 'registerDevice', [arg0]);

}
