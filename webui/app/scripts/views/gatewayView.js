define(['backbone', 'hbs!tmpl/gateway'], function(Backbone, GatewayTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: GatewayTmpl,
        className: 'container',
        initialize: function() {
            this.model.on('error', this.onError, this);
        },
        onError: function(){
            this.$('div.alert').show();
            this.$('button').button('reset');
        },
        events: {
            'click #feeBtn':'handleFee',
            'click #withdrawalBtn':'handleWithdrawal'
        },
        handleFee: function(e){
            e.preventDefault();
            $(e.target).button('loading');
            var fee = this.$('#feeVal').val();
            this.$('#error').hide();
            this.$('#success').hide();
            if (fee !== this.model.get('fee')){
                this.$('#feeBtn').attr('disabled', true);
                this.model.set('fee',fee);
                this.model.once('error', function(){
                    this.$('#error').show();
                    this.$('#feeBtn').removeAttr('disabled');
                }, this);
                this.model.once('sync', function(){
                    this.$('#success').show();
                    this.$('#feeBtn').removeAttr('disabled');
                }, this);
                this.model.save();
            }
        },
        handleWithdrawal: function(e){
            e.preventDefault();
            $(e.target).button('loading');
            $(e.target).parent().parent().parent().children('.alert').show();
        },
        onShow:function () {
            this.$('div.alert').hide();
            this.$('#feeVal').val(this.model.get('fee'));
            this.$('button').button('reset');
        }
    });
});