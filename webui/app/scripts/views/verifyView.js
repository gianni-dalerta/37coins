define(['backbone', 'communicator', 'views/validateView', 'hbs!tmpl/verify'], function(Backbone, Communicator, ValidateView, VerifyTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: VerifyTmpl,
        className: 'container',
        initialize: function(opt) {
            this.next = opt.next;
            this.model.on('sync', this.onSync, this);
            this.model.on('error', this.onError, this);
        },
        onSync: function(){
            Communicator.mediator.trigger('app:verify', this.next);
        },
        onError: function(){
            this.$('div.alert').show();
            this.$('button').button('reset');
        },
        events: {
            'click #valBtn':'handleValidate'
        },
        handleValidate: function(e) {
            e.preventDefault();
            $(e.target).button('loading');
            var mobile = this.$('#mobVal').val();
            this.model.set('mobile',mobile);
            this.model.set('locale',window.opt.lng);
            this.model.save();
        },
        onShow:function () {
            this.$('.alert').alert();
            this.$('div.alert').hide();
            this.$('button').button('reset');
        }
    });
});