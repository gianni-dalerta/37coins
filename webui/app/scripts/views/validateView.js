define([
    'backbone',
    'communicator',
    'hbs!tmpl/validate'
],
function(Backbone, Communicator, ValidateTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: ValidateTmpl,
        className: 'container',
        initialize: function() {
            this.model.on('sync', this.onSync, this);
            this.model.on('error', this.onError, this);
        },
        onSync: function(){
            Communicator.mediator.trigger('app:verify');
        },
        onError: function(){
            this.$('.alert').css('display','');
            this.$('.alert').addClass('in');
            this.$('button.btn-primary').button('reset');
        },
        handleClose: function(e){
            var alert = $(e.target).parent();
            alert.one(window.transEvent(), function(){
                alert.css('display', 'none');
            });
            alert.removeClass('in');
        },

        /* Ui events hash */
        events: {
            'click .close': 'handleClose',
        },

        handleConfirm: function() {
            this.$('button.btn-primary').button('loading');
            var code = this.$('input[name="code"]').val();
            this.model.set('code',code);
            this.model.save();
        },
        onShow:function () {
            this.$('.alert').css('display', 'none');
            var jForm = this.$('form');
            var self = this;
            jForm.validate({
                rules: {
                    code: {
                        required: true,
                        digits: true,
                        minlength: 5,
                        maxlength: 5
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
                    self.handleConfirm();
                },
                errorPlacement: function(error, element) {
                    error.insertAfter(element);
                }
            });
        }
    });
});