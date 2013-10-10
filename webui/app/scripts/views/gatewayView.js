define(['underscoreM', 'marionette', 'templates'], function(_, Marionette, templates) {
    'use strict';
    return Marionette.ItemView.extend({
        template: _.template(templates.gateway),
        className: 'container',
        initialize: function(opt) {
            //something
        },
        events: {
            'click #valBtn':'handleValidate',
            'click #cfmBtn':'handleConfirm'
        },
        handleValidate: function(e) {
            e.preventDefault();
            var mobile = $('#mobVal').val();
            this.model.set('mobile',mobile);
            this.model.set('locale',window.opt.lng);
            this.model.save();
        },
        handleConfirm: function(e) {
            e.preventDefault();
            var code = $('#cfmVal').val();
            this.model.set('code',code);
            this.model.save();
        },
        onShow:function () {
            if (!this.model.get('mobile')){
                //show validation form
            }
        }
    });
});