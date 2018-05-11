import groovy.transform.Field

def defVal (value, defaultValue) {
  if (value == null || value == []) {
    return defaultValue
  } else {
    if (value instanceof java.util.LinkedHashMap) {
      return value + defaultValue
    } else {
      return value
    }
  }
}

@Field def defaultEnv = ["CI=true"]
@Field def defaultTest = "go test -v ./..."

def call(opts = []) {
  def env = defVal(opts['env'], defaultEnv)
  def test = defVal(opts['test'], defaultTest)
  timeout(time: 1, unit: 'HOURS') {
    stage('tests') {
      parallel(
        windows: {
          node(label: 'windows') {
            ansiColor('xterm') {
              def root = tool name: '1.10.2', type: 'go'
              def jobNameArr = "${JOB_NAME}"
              def jobName = jobNameArr.split("/")[0..1].join("\\\\").toLowerCase()
              def originalWs = "${WORKSPACE}"
              ws("${originalWs}\\src\\github.com\\${jobName}") {
                def goEnv = ["GOROOT=${root}", "GOPATH=${originalWs}", "PATH=$PATH;${root}\\bin;${originalWs}\\bin"]
                withEnv(goEnv + env) {
                  bat 'go get -v github.com/whyrusleeping/gx'
                  bat 'go get -v github.com/whyrusleeping/gx-go'
                  bat 'go get -v github.com/jstemmer/go-junit-report'
                  checkout scm
                  bat 'gx --verbose install --global'
                  bat 'gx-go rewrite'
                  try {
                    bat test + ' > output & type output'
                    bat 'type output | go-junit-report > junit-report-windows.xml'
                  } catch (err) {
                    throw err
                  } finally {
                    junit allowEmptyResults: true, testResults: 'junit-report-*.xml'
                  }
                }
              }
            }
          }
        },
        linux: {
          node(label: 'linux') {
            ansiColor('xterm') {
              def root = tool name: '1.10.2', type: 'go'
              def jobNameArr = "${JOB_NAME}"
              def jobName = jobNameArr.split("/")[0..1].join("/").toLowerCase()
              def originalWs = "${WORKSPACE}"
              ws("${originalWs}/src/github.com/${jobName}") {
                def goEnv = ["GOROOT=${root}", "GOPATH=${originalWs}", "PATH=$PATH:${root}/bin:${originalWs}/bin"]
                withEnv(goEnv + env) {
                  sh 'go get -v github.com/whyrusleeping/gx'
                  sh 'go get -v github.com/whyrusleeping/gx-go'
                  sh 'go get -v github.com/jstemmer/go-junit-report'
                  checkout scm
                  sh 'gx --verbose install --global'
                  sh 'gx-go rewrite'
                  try {
                    sh test + ' 2>&1 | tee output'
                    sh 'cat output | go-junit-report > junit-report-linux.xml'
                  } catch (err) {
                    throw err
                  } finally {
                    junit allowEmptyResults: true, testResults: 'junit-report-*.xml'
                  }
                }
              }
            }
          }
        },
        macOS: {
          node(label: 'macos') {
            ansiColor('xterm') {
              def root = tool name: '1.10.2', type: 'go'
              def jobNameArr = "${JOB_NAME}"
              def jobName = jobNameArr.split("/")[0..1].join("/").toLowerCase()
              def originalWs = "${WORKSPACE}"
              ws("${originalWs}/src/github.com/${jobName}") {
                def goEnv = ["GOROOT=${root}", "GOPATH=${originalWs}", "PATH=$PATH:${root}/bin:${originalWs}/bin"]
                withEnv(goEnv + env) {
                  sh 'go get -v github.com/whyrusleeping/gx'
                  sh 'go get -v github.com/whyrusleeping/gx-go'
                  sh 'go get -v github.com/jstemmer/go-junit-report'
                  checkout scm
                  sh 'gx --verbose install --global'
                  sh 'gx-go rewrite'
                  try {
                    sh test + ' 2>&1 | tee output'
                    sh 'cat output | go-junit-report > junit-report-macos.xml'
                  } catch (err) {
                    throw err
                  } finally {
                    junit 'junit-report-*.xml'
                  }
                }
              }
            }
          }
        }
      )
    }
  }
}
