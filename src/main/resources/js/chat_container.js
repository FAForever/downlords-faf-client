isScrolledToBottom = true;
window.onscroll = function (e) {
  isScrolledToBottom = window.scrollY+window.innerHeight + 20 >= document.documentElement.scrollHeight;
}

function link(input) {
  return Autolinker.link(input, {
      email: false,
      phone: false,
      twitter: false,
      replaceFn: function(autolinker, match) {
        return new Autolinker.HtmlTag( {
          tagName: "a",
          attrs: {
            "href": "javascript:void(0);",
            "onClick": "chatTab.openUrl('"+match.getUrl()+"')",
            "onMouseOver": "chatTab.previewUrl('"+match.getUrl()+"')",
          },
          innerHtml: match.getAnchorText()
        } );
      }
    }
  );
}

function showPlayerInfo(node) {
  chatTab.playerInfo(node.textContent);
}

function hidePlayerInfo(node) {
  chatTab.hidePlayerInfo();
}

function scrollToBottomIfDesired() {
  if (isScrolledToBottom) {
    window.scrollTo(0, document.documentElement.scrollHeight);
  }
}
