define([
    'backbone',
    'hbs!tmpl/feeView_tmpl',
],
function(Backbone, FeeTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: FeeTmpl,
        className: 'gwLayout',
        initialize: function() {
            this.model.on('error', this.onError, this);
            this.model.on('sync', this.onSuccess, this);
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
        handleFee: function(){
            this.$('button').button('loading');
            var fee = this.$('#feeInput').val();
            if (fee !== this.model.get('fee')){
                this.$('#feeBtn').attr('disabled', true);
                sessionStorage.setItem('fee',fee);
                this.model.set('fee',fee);
                this.model.save();
            }
        },
        onShow:function () {
            this.$('#feeInput').val(this.model.get('fee'));
            this.$('.alert').css('display', 'none');
            var jForm = this.$('form');
            var self = this;
            jForm.validate({
                rules: {
                    fee: {
                        required: true,
                        number: true
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
                    self.handleFee();
                },
                errorPlacement: function(error, element) {
                    error.insertAfter(element);
                }
            });
        }
    });
});