from __future__ import print_function

import json
import sys
import urllib.request

GITHUB_BASE_URL = "https://api.github.com/repos/FAForever/downlords-faf-client"


def call_github(path):
    request = urllib.request.Request(GITHUB_BASE_URL + path)
    request.add_header('User-Agent', 'micheljung')
    return urllib.request.urlopen(request)


def build_release_body(version_string):
    response = call_github("/milestones?state=all&per_page=100&direction=desc")
    milestones = json.loads(response.read().decode('utf-8'))
    milestone_number = None
    for milestone in milestones:
        if version_string == milestone['title']:
            milestone_number = milestone['number']

    if not milestone_number:
        print("WARN No milestone '{0}', returning empty body".format(version_string), file=sys.stderr)
        return ""

    body = ""
    response = call_github("/issues?state=closed&sort=created&direction=asc&milestone=" + str(milestone_number))
    issues = json.loads(response.read().decode('utf-8'))
    for issue in issues:
        body += "* Fixed #{0}: {1}\n".format(issue['number'], issue['title'])

    return body


if __name__ == '__main__':
    print(build_release_body(sys.argv[1]))
