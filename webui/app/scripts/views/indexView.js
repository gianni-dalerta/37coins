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
        }
    });
});