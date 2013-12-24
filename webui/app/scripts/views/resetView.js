define([
	'backbone',
	'communicator',
	'hbs!tmpl/resetView_tmpl',
	'hbs!tmpl/resetCompletedView_tmpl'
],
function( Backbone, Communicator, ResetTmpl, ResetCompleteTmpl  ) {
    'use strict';

	/* Return a ItemView class definition */
	return Backbone.Marionette.ItemView.extend({

		initialize: function() {
			console.log('initialize a Reset ItemView');
			this.model.on('error', this.onError, this);
			this.model.on('sync', this.onSuccess, this);
		},

		getTemplate: function(){
		    if (this.fetched){
		        return ResetCompleteTmpl;
		    } else {
		        return ResetTmpl;
		    }
		},

		onSuccess: function(){
			this.fetched = true;
			this.render();
		},

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
			$(e.target).button('loading');
			var user = this.$('#exampleInputEmail1').val();
			this.model.set('email',user);
			this.model.save();
		},

		/* on render callback */
		onShow: function() {
			if (!this.fetched){
				this.$('.alert').css('display', 'none');
			}
		}
	});

});
