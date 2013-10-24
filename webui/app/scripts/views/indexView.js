define(['underscoreM', 'marionette', 'templates'], function(_, Marionette, templates) {
    'use strict';
    return Marionette.ItemView.extend({
        template: _.template(templates.index),
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