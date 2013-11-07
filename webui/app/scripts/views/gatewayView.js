define(['backbone', 'hbs!tmpl/gateway'], function(Backbone, GatewayTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: GatewayTmpl,
        className: 'container',
        initialize: function() {
            //something
        },
        events: {
            'click #valBtn':'handleValidate',
            'click #cfmBtn':'handleConfirm',
            'click #feeBtn':'handleFee',
            'click a.close':'handleAlert',
            'click #withdrawalBtn':'handleWithdrawal'
        },
        handleValidate: function(e) {
            e.preventDefault();
            var mobile = this.$('#mobVal').val();
            this.model.set('mobile',mobile);
            this.model.set('locale',window.opt.lng);
            this.model.save();
        },
        handleConfirm: function(e) {
            e.preventDefault();
            var code = this.$('#cfmVal').val();
            this.model.set('code',code);
            this.model.save();
        },
        handleFee: function(e){
            e.preventDefault();
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
        handleAlert: function(e){
            e.preventDefault();
            $(e.target).parent().hide();
        },
        handleWithdrawal: function(e){
            e.preventDefault();
            $(e.target).parent().parent().parent().children('.alert').show();
        },
        onShow:function () {
            this.$('div.alert').hide();
            if (!this.model.get('mobile')){
                //show only validate phone form
                this.$('#cfmFrm').hide();
                this.$('#cnfFrm').hide();
                this.$('#statusFrm').hide();
                this.$('#valFrm').show();
            }else if (!this.model.get('fee')){
                //show only confirm phone form
                this.$('#cfmFrm').show();
                this.$('#cnfFrm').hide();
                this.$('#statusFrm').hide();
                this.$('#valFrm').hide();
            }else {
                //show only config and status views
                this.$('#cfmFrm').hide();
                this.$('#cnfFrm').show();
                this.$('#statusFrm').show();
                this.$('#valFrm').hide();
                this.$('#feeVal').val(this.model.get('fee'));
            }
        }
    });
});