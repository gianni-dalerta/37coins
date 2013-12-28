define([
    'backbone',
    'hbs!tmpl/balanceView_tmpl',
],
function(Backbone, BalanceTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: BalanceTmpl,
        className: 'gwLayout',
        initialize: function() {
            this.model.on('error', this.onError, this);
            this.model.on('sync', this.onSuccess, this);
            this.iid = window.setInterval(this.getBalance, 10000);
        },
        close: function(){
            window.clearInterval(this.iid);
        },
        getBalance: function(){
            var cred = sessionStorage.getItem('credentials');
            var up = $.parseJSON(cred);
            var self = this;
            $.ajax({
                type: 'GET',
                url: window.opt.basePath + '/api/gateway/balance',
                dataType: 'json',
                async: false,
                beforeSend: function(xhr) {
                    xhr.setRequestHeader('Authorization', 'Basic ' + btoa(up.username + ':' + up.password));
                },
                success: function(data){
                    self.$('span.spin').replaceWith('<strong> ' + data.balance + ' BTC</string>');
                    window.clearInterval(self.iid);
                }
            });
        },
        onError: function(){
            this.$('#errorAlert').css('display','');
            this.$('#errorAlert').addClass('in');
            this.$('button').button('reset');
        },
        onSuccess: function(){
            this.$('#successAlert').css('display','');
            this.$('#successAlert').addClass('in');
            this.$('button').button('reset');
        },
        handleClose: function(e){
            var alert = $(e.target).parent();
            alert.one(window.transEvent(), function(){
                alert.css('display', 'none');
            });
            alert.removeClass('in');
        },
        events: {
            'click .close': 'handleClose',
        },
        handleWithdrawal: function(){
            this.$('button').button('loading');
            this.handleClose({target:this.$('#successAlert:first-child')[0]});
            this.handleClose({target:this.$('#errorAlert:first-child')[0]});
            var amount = this.$('#amountInput').val();
            var address = this.$('#addressInput').val();
            this.model.set('amount',amount);
            this.model.set('address',address);
            this.model.save();
        },
        onShow:function () {
            this.$('.alert').css('display', 'none');
            var jForm = this.$('form');
            var self = this;
            jForm.validate({
                rules: {
                    amount: {
                        required: true,
                        number: true
                    },
                    address: {
                        required: true,
                        minlength: 20
                    }
                },
                highlight: function(element) {
                    $(element).closest('.form-group').addClass('has-error');
                },
                unhighlight: function(element) {
                    $(element).closest('.form-group').removeClass('has-error');
                },
                errorElement: 'span',
                errorClass: 'help-block',
                submitHandler: function() {
                    self.handleWithdrawal();
                },
                errorPlacement: function(error, element) {
                    error.insertAfter(element);
                }
            });
        }
    });
});