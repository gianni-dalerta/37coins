define([
    'backbone',
    'hbs!tmpl/balanceView_tmpl',
],
function(Backbone, BalanceTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: BalanceTmpl,
        className: 'container',
        initialize: function() {
            this.model.on('error', this.onError, this);
            this.model.on('sync', this.onSuccess, this);
        },
        onSuccess: function(){
            console.log(this.model.get('value'));
        },
        onError: function(){
            this.$('div.alert').show();
            this.$('button').button('reset');
        },
        events: {
            'click #withdrawalBtn':'handleWithdrawal'
        },
        handleWithdrawal: function(e){
            e.preventDefault();
            $(e.target).button('loading');
            $(e.target).parent().parent().parent().children('.alert').show();
        },
        onShow:function () {
            this.$('div.alert').hide();
            this.$('button').button('reset');
        }
    });
});