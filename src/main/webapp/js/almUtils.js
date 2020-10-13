$.noConflict();
jQuery( document ).ready(function($) {

    setTimeout(function () {
        validateTaskForm()}, 200);

    $('input[name="runfromalm.isSSOEnabled"]').click(function () {
            selectCredentialsType(this,  $('input[name="runfromalm.isSSOEnabled"]').index(this));
    });

    function validateTaskForm(){

        var checkBoxList = $('input[name="runfromalm.isSSOEnabled"]');

        for(var i = 0; i < checkBoxList.length; i++){
            selectCredentialsType(checkBoxList[i], i);
        }


    }

   function selectCredentialsType(element, index){
        if (element.checked) {//use SSO
            $('.noSSOCredentials')[index].hide();
            $('.ssoCredentials')[index].show();
        } else {//do not use SSO

            $('.noSSOCredentials')[index].show();
            $('.ssoCredentials')[index].hide();
        }
    }
});

