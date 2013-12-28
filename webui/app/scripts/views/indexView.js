define([
    'backbone',
    'hbs!tmpl/index',
    'views/gatewayPreView'
],
function(Backbone, IndexTmpl, GatewayView) {
    'use strict';
    return Backbone.Marionette.CompositeView.extend({
        itemView: GatewayView,
        itemViewContainer: '#gwTable',
        template: IndexTmpl,
        className: 'container',
        initialize: function() {
            console.dir(this.model);
        },
        events: {
            'click a.btn-success':'handleClick'
        },
        handleClick: function(e){
            e.preventDefault();
            var jt = $('div.jumbotron');
            jt.slideUp();
        }
    });
});