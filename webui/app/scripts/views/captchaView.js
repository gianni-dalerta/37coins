define([
	'backbone',
	'hbs!tmpl/captchaView_tmpl',
	'recaptcha'
],
function( Backbone, CaptchaTmpl, Recaptcha) {
    'use strict';

	/* Return a ItemView class definition */
	return Backbone.Marionette.ItemView.extend({

		initialize: function(opt) {
			this.next = opt.next;
			this.controller = opt.controller;
		},
		
		template: CaptchaTmpl,


		/* ui selector cache */
		ui: {},

		/* Ui events hash */
		events: {
            'click button.btn-primary':'handleCaptcha',
            'click button.close': 'handleClose'
        },


        handleCaptcha: function(e) {
			e.preventDefault();
			var chal = Recaptcha.get_challenge();
			var resp = Recaptcha.get_response();
			Recaptcha.destroy();
			$(e.target).button('loading');
			var self = this;
			$.post( window.opt.basePath + '/account/captcha',
				{chal: chal, resp: resp},
				function( data ) {
					self.controller.ticket = data.value;
					self.next();
			    }
			).fail(function() {
				Recaptcha.create(window.opt.captchaPubKey, self.$('#recaptcha_div')[0], {
					tabindex: 4,
					theme: 'clean'
				});
				self.$('.alert').css('display','');
				self.$('.alert').addClass('in');
				self.$('button.btn-primary').button('reset');
			});
        },

        handleClose: function(e){
            var alert = $(e.target).parent();
            alert.one(window.transEvent(), function(){
                alert.css('display', 'none');
            });
            alert.removeClass('in');
        },

        onShow: function(){
			this.$('.alert').css('display', 'none');
			var self = this;
            Recaptcha.create(window.opt.captchaPubKey, self.$('#recaptcha_div')[0], {
				tabindex: 4,
				theme: 'clean'
			});
        }

	});

});
