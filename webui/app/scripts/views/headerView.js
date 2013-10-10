define(['underscoreM', 'marionette', 'templates'], function(_, Marionette, templates) {
    'use strict';
    return Marionette.ItemView.extend({
        template: _.template(templates.header),
        className: 'transp',
        onShow:function () {
			console.log($('ul.nav'));
			$('ul.nav').tab();
        }
    });
});