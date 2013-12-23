define(['backbone', 'communicator', 'hbs!tmpl/validate'], function(Backbone, Communicator, ValidateTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: ValidateTmpl,
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
            'click #cfmBtn':'handleConfirm'
        },
        handleConfirm: function(e) {
            e.preventDefault();
            $(e.target).button('loading');
            var code = this.$('#cfmVal').val();
            this.model.set('code',code);
            this.model.save();
        },
        onShow:function () {
            this.$('div.alert').hide();
            this.$('button').button('reset');
        }
    });
});