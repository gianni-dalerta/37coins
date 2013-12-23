define([
	'backbone',
	'communicator',
	'hbs!tmpl/resetView_tmpl'
],
function( Backbone, Communicator, ResetTmpl  ) {
    'use strict';

	/* Return a ItemView class definition */
	return Backbone.Marionette.ItemView.extend({

		initialize: function() {
			console.log('initialize a Reset ItemView');
			this.model.on('error', this.onError, this);
		},

		template: ResetTmpl,
        
        onError: function(model, response){
			if (response.status===400){
				location.reload();
			}
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

		/* ui selector cache */
		ui: {},

		/* Ui events hash */
		events: {
			'click button.btn-primary':'handleReset',
			'click .close': 'handleClose',
		},

		handleReset: function(e){
			e.preventDefault();
			var user = this.$('input:text').val();
			this.model.set('email',user);
			this.model.save();
		},

		/* on render callback */
		onRender: function() {
			this.$('.alert').css('display', 'none');
		}
	});

});
