define(['backbone', 'hbs!tmpl/index'], function(Backbone, IndexTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: IndexTmpl,
        className: 'container',
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