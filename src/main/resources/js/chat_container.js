isScrolledToBottom = true;
window.onscroll = function (e) {
  isScrolledToBottom = window.scrollY + window.innerHeight + 20 >= document.documentElement.scrollHeight;
};

function link(input) {
  return Autolinker.link(input, {
        email: false,
        phone: false,
        twitter: false,
        replaceFn: function (autolinker, match) {
          return new Autolinker.HtmlTag({
            tagName: "a",
            attrs: {
              "href": "javascript:void(0);",
              "onClick": "chatTab.openUrl('" + match.getUrl() + "')",
              "onMouseOver": "chatTab.previewUrl('" + match.getUrl() + "')",
              "onMouseOut": "chatTab.hideUrlPreview()"
            },
            innerHtml: match.getAnchorText()
          });
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

function messagePlayer(node) {
  chatTab.openPrivateMessageTab(node.textContent);
}

function scrollToBottomIfDesired() {
  if (isScrolledToBottom) {
    window.scrollTo(0, document.documentElement.scrollHeight);
  }
}

function setRandomColors(userListString) {
  var userList = JSON.parse(userListString);

  for (var user in userList) {
    if (userList.hasOwnProperty(user)) {
      var messages = document.getElementsByClassName(user);
      for (var i = 0; i < messages.length; i++) {
        messages[i].style.color = userList[user];
      }
    }
  }
}

function removeRandomColors() {
  var messages = document.getElementsByClassName("chat-message");
  for (var i = 0; i < messages.length; i++) {
    messages[i].style.color = "";
  }
}