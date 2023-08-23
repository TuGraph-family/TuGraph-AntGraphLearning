function build_api_doc()
{
    # sphinx==4.3.1 is not recommended for its wrong search result.
    pip install sphinx==4.1.2 docutils==0.15.2 recommonmark sphinx_rtd_theme sphinx_markdown_tables==0.0.16 torch==1.12.1 -i https://pypi.antfin-inc.com/simple
    find source -name "*.rst" | grep -v index.rst | xargs rm -rf
    excludes=`python exclude.py`
    sphinx-apidoc -f -o source/ ../agl/python/ $excludes
    #python format_rst.py source
    make clean
    make html
}

build_api_doc