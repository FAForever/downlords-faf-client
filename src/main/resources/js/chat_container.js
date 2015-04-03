function link(input) {
  return Autolinker.link(input, {
      email: false,
      phone: false,
      twitter: false,
      replaceFn: function(autolinker, match) {
        // channelTab is a reference to the java instance
        return new Autolinker.HtmlTag( {
          tagName: "a",
          attrs: {
            "href": "#",
            "onclick": "channelTab.openUrl('"+match.getUrl()+"')"
          },
          innerHtml: match.getAnchorText()
        } );
      }
    }
  );
}
