// templates
define(function(require) {
    'use strict';
    return {
        index: require('text!http://staging.37coins.com:4080/res/scripts/templates/index.htm'),
        header: require('text!http://staging.37coins.com:4080/res/scripts/templates/header.htm'),
        footer: require('text!http://staging.37coins.com:4080/res/scripts/templates/footer.htm'),
        gateway: require('text!http://staging.37coins.com:4080/res/scripts/templates/gateway.htm'),
        login: require('text!http://staging.37coins.com:4080/res/scripts/templates/login.htm')
    };
});
