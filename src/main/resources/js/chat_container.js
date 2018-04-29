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
function showClanInfo(node) {
  chatTab.clanInfo(node.textContent);
}

function hideClanInfo() {
  chatTab.hideClanInfo();
}

function showClanWebsite(node) {
  chatTab.showClanWebsite(node.textContent);
}

function showCountryInfo(node) {
  chatTab.countryInfo(node.getAttribute("src"));
}

function hideCountryInfo() {
  chatTab.hideCountryInfo();
}

function showPlayerInfo(node) {
  chatTab.playerInfo(node.textContent);
}

function hidePlayerInfo() {
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

function setAllMessageColors(userListString) {
  var userList = JSON.parse(userListString);

  for (var user in userList) {
    if (userList.hasOwnProperty(user)) {
      updateUserMessageColor(user, userList[user]);
    }
  }
}

function removeAllMessageColors() {
  var messages = document.getElementsByClassName("chat-message");
  for (var i = 0; i < messages.length; i++) {
    messages[i].style.color = "";
  }
}

function updateUserMessageColor(user, color) {
  var messages = document.getElementsByClassName("user-" + user);
  for (var i = 0; i < messages.length; i++) {
    messages[i].style.color = color;
  }
}

function setUserMessageClass(user, cssClass) {
  var userMessages = document.getElementsByClassName(user);
  for (var i = 0; i < userMessages.length; i++) {
    userMessages[i].classList.add(cssClass);
  }
}

function removeUserMessageClass(user, cssClass) {
  var userMessages = document.getElementsByClassName(user);
  for (var i = 0; i < userMessages.length; i++) {
    userMessages[i].classList.remove(cssClass);
  }
}

function updateUserMessageDisplay(user, display) {
  var userMessages = document.getElementsByClassName("chat-section-" + user);
  for (var i = 0; i < userMessages.length; i++) {
    userMessages[i].style.display = display;
  }
}


function highlightText(text) {
  $('#chat-container').removeHighlight().highlight(text);
}

function removeHighlight() {
  $('#chat-container').removeHighlight();
}