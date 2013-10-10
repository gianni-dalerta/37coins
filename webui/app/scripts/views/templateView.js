define(['underscoreM', 'marionette'], function(_, Marionette) {
    'use strict';
    return Marionette.ItemView.extend({
        className: 'fromTemplate',
        initialize: function(opt) {
			this.tmplt = opt.tmplt;
        },
        getTemplate: function(){
			return _.template(this.tmplt);
        },
        onShow:function () {
            //do something
        }
    });
});